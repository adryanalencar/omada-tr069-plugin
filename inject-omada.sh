#!/usr/bin/env bash
# ==============================================================================
# Script de Injeção do Mini-Framework React no TP-Link Omada Controller
# ==============================================================================
set -e

# Cores para terminal
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # Sem cor

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

if [[ -d "$SCRIPT_DIR/omada-react-framework" ]]; then
    FRAMEWORK_DIR="$SCRIPT_DIR/omada-react-framework"
elif [[ -d "$SCRIPT_DIR/../omada-react-framework" ]]; then
    FRAMEWORK_DIR="$SCRIPT_DIR/../omada-react-framework"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
elif [[ -f "$SCRIPT_DIR/package.json" ]]; then
    FRAMEWORK_DIR="$SCRIPT_DIR"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
else
    FRAMEWORK_DIR="$SCRIPT_DIR/omada-react-framework"
fi

PLUGIN_DIST="$FRAMEWORK_DIR/dist/omada-react-plugin.js"

# Variáveis configuráveis
SKIP_BUILD=false
RESTORE_MODE=false
CUSTOM_JAR=""
RESTART_SERVICE=false

print_banner() {
    echo -e "${BLUE}${BOLD}"
    echo "=========================================================="
    echo "   TP-Link Omada Controller - React Plugin Injector      "
    echo "=========================================================="
    echo -e "${NC}"
}

usage() {
    print_banner
    echo -e "Uso: $0 [opções] [caminho_para_o_omada-web.jar]"
    echo ""
    echo "Opções:"
    echo "  -t, --target <path>       Caminho para o omada-web-*.jar alvo"
    echo "  --no-build                Pula o passo 'npm run build'"
    echo "  -r, --restore             Restaura o JAR original a partir do backup (.bak)"
    echo "  --restart                 Tenta reiniciar o serviço do Omada (systemctl)"
    echo "  -h, --help                Exibe esta ajuda"
    echo ""
    echo "Exemplos:"
    echo "  $0                                            # Auto-detecta JAR e injeta"
    echo "  $0 /opt/tplink/EAPController/lib/omada-web-*.jar"
    echo "  $0 --restore                                  # Desfaz a injeção"
    exit 0
}

# Parse argumentos
while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help)
            usage
            ;;
        --no-build)
            SKIP_BUILD=true
            shift
            ;;
        -r|--restore)
            RESTORE_MODE=true
            shift
            ;;
        --restart)
            RESTART_SERVICE=true
            shift
            ;;
        -t|--target)
            CUSTOM_JAR="$2"
            shift 2
            ;;
        *)
            if [[ -z "$CUSTOM_JAR" ]]; then
                CUSTOM_JAR="$1"
            fi
            shift
            ;;
    esac
done

find_target_jar() {
    if [[ -n "$CUSTOM_JAR" ]]; then
        if [[ -f "$CUSTOM_JAR" ]]; then
            echo "$CUSTOM_JAR"
            return 0
        else
            echo -e "${RED}[✗] Arquivo JAR informado não foi encontrado: $CUSTOM_JAR${NC}" >&2
            exit 1
        fi
    fi

    # Candidatos padrão
    local candidates=(
        "$PROJECT_ROOT/packages/rootfs/opt/tplink/EAPController/lib/omada-web-*.jar"
        "/opt/tplink/EAPController/lib/omada-web-*.jar"
        "/opt/tplink/EAPController/lib/omada-web*.jar"
    )

    for pattern in "${candidates[@]}"; do
        # Expande glob
        for file in $pattern; do
            if [[ -f "$file" ]]; then
                echo "$file"
                return 0
            fi
        done
    done

    echo -e "${RED}[✗] Não foi possível localizar automaticamente o omada-web-*.jar.${NC}" >&2
    echo -e "Por favor, informe o caminho manualmente: $0 --target /caminho/omada-web-*.jar" >&2
    exit 1
}

# ------------------------------------------------------------------------------
# MODO RESTAURAÇÃO
# ------------------------------------------------------------------------------
if [[ "$RESTORE_MODE" = true ]]; then
    print_banner
    TARGET_JAR=$(find_target_jar)
    BACKUP_JAR="${TARGET_JAR}.bak"

    echo -e "${BLUE}[i] Alvo selecionado:${NC} $TARGET_JAR"

    if [[ -f "$BACKUP_JAR" ]]; then
        echo -e "${YELLOW}[!] Restaurando backup original de: $BACKUP_JAR...${NC}"
        cp -f "$BACKUP_JAR" "$TARGET_JAR"
        echo -e "${GREEN}[✓] Sucesso! O arquivo original foi restaurado.${NC}"
        
        if [[ "$RESTART_SERVICE" = true ]]; then
            echo -e "${BLUE}[i] Reiniciando serviço do Omada...${NC}"
            if command -v systemctl &>/dev/null; then
                systemctl restart tpeap 2>/dev/null || systemctl restart tphostd 2>/dev/null || /opt/tplink/EAPController/bin/control.sh restart || true
            else
                /opt/tplink/EAPController/bin/control.sh restart || true
            fi
        fi
        exit 0
    else
        echo -e "${RED}[✗] Arquivo de backup não encontrado: $BACKUP_JAR${NC}"
        exit 1
    fi
fi

# ------------------------------------------------------------------------------
# MODO INJEÇÃO
# ------------------------------------------------------------------------------
print_banner

# 1. Compilação do React Plugin
if [[ "$SKIP_BUILD" = false ]]; then
    echo -e "${BLUE}[1/5] Compilando o mini-framework React...${NC}"
    if [[ ! -d "$FRAMEWORK_DIR" ]]; then
        echo -e "${RED}[✗] Diretório omada-react-framework não encontrado em: $FRAMEWORK_DIR${NC}"
        exit 1
    fi

    pushd "$FRAMEWORK_DIR" > /dev/null
    
    # Se nvm existir no sistema e npm não estiver no PATH direto
    if ! command -v npm &>/dev/null; then
        if [[ -f "$HOME/.nvm/nvm.sh" ]]; then
            source "$HOME/.nvm/nvm.sh"
            nvm use v22.23.2 2>/dev/null || true
        fi
    fi

    if ! command -v npm &>/dev/null; then
        echo -e "${RED}[✗] npm não encontrado no PATH.${NC}"
        popd > /dev/null
        exit 1
    fi

    npm run build
    popd > /dev/null
    echo -e "${GREEN}[✓] Build concluído com sucesso!${NC}"
else
    echo -e "${YELLOW}[1/5] Pulando compilação (usando dist existente)...${NC}"
fi

if [[ ! -f "$PLUGIN_DIST" ]]; then
    echo -e "${RED}[✗] Arquivo compilado não encontrado: $PLUGIN_DIST${NC}"
    exit 1
fi

# 2. Localização do JAR
echo -e "${BLUE}[2/5] Localizando pacote do Omada Web...${NC}"
TARGET_JAR=$(find_target_jar)
echo -e "${GREEN}[✓] JAR encontrado:${NC} $TARGET_JAR"

# 3. Criação de Backup de Segurança
echo -e "${BLUE}[3/5] Verificando backup de segurança...${NC}"
BACKUP_JAR="${TARGET_JAR}.bak"
if [[ ! -f "$BACKUP_JAR" ]]; then
    echo -e "${YELLOW}[i] Criando cópia de segurança em: $BACKUP_JAR${NC}"
    cp -p "$TARGET_JAR" "$BACKUP_JAR"
    echo -e "${GREEN}[✓] Backup criado com sucesso.${NC}"
else
    echo -e "${GREEN}[✓] Backup existente preservado:${NC} $BACKUP_JAR"
fi

# 4. Injeção no HTML e Empacotamento
echo -e "${BLUE}[4/5] Modificando static/index.html e injetando bundle...${NC}"

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

pushd "$WORK_DIR" > /dev/null

# Cria estrutura de pastas correspondente à interna do JAR
mkdir -p static/js

# Extrai o index.html original do JAR
unzip -q -p "$TARGET_JAR" static/index.html > static/index.html

# Copia o bundle compilado do React
cp "$PLUGIN_DIST" static/js/omada-react-plugin.js

# Verifica se a tag do script já existe no index.html
SCRIPT_TAG='<script type="text/javascript" src="js/omada-react-plugin.js"></script>'

if grep -q "omada-react-plugin.js" static/index.html; then
    echo -e "${YELLOW}[i] A tag do script já estava presente no index.html. Apenas atualizando o bundle JS.${NC}"
else
    echo -e "${BLUE}[i] Inserindo tag <script> antes de </body>...${NC}"
    # Substitui </body> por <script ...></body>
    python3 -c "
with open('static/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

tag = '''    <script type=\"text/javascript\" src=\"js/omada-react-plugin.js\"></script>\n</body>'''
if '</body>' in content:
    new_content = content.replace('</body>', tag, 1)
    with open('static/index.html', 'w', encoding='utf-8') as f:
        f.write(new_content)
    print('Tag injetada com sucesso no HTML!')
else:
    raise Exception('Tag </body> não encontrada no index.html')
"
fi

# Atualiza o JAR diretamente com o zip
echo -e "${BLUE}[i] Atualizando o arquivo JAR...${NC}"
zip -u "$TARGET_JAR" static/index.html static/js/omada-react-plugin.js > /dev/null

popd > /dev/null

echo -e "${GREEN}[✓] Injeção concluída no JAR!${NC}"

# 5. Reinicialização de Serviço (se solicitado)
echo -e "${BLUE}[5/5] Finalização...${NC}"
if [[ "$RESTART_SERVICE" = true ]]; then
    echo -e "${YELLOW}[!] Reiniciando serviço do Omada...${NC}"
    if command -v systemctl &>/dev/null; then
        systemctl restart tpeap 2>/dev/null || systemctl restart tphostd 2>/dev/null || /opt/tplink/EAPController/bin/control.sh restart || true
    else
        /opt/tplink/EAPController/bin/control.sh restart || true
    fi
fi

echo -e "${GREEN}${BOLD}"
echo "=========================================================="
echo " [✓] Plugin React injetado com sucesso no Omada Controller! "
echo "=========================================================="
echo -e "${NC}"
echo -e "JAR modificado: ${BOLD}$TARGET_JAR${NC}"
echo -e "Script injetado: ${BOLD}static/js/omada-react-plugin.js${NC}"
echo ""
echo -e "${YELLOW}Dica de cache:${NC} No navegador, lembre-se de limpar os dados/IndexedDB ou dar Ctrl+F5 (Cmd+Shift+R)."
echo -e "Para reverter ao estado original a qualquer momento: ${BOLD}$0 --restore${NC}"
