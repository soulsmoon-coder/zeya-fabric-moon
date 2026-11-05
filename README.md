# ZeyaMoon — A Lua do Eclipse Primordial

> “Nascida antes da luz, despertada no eclipse e destinada a existir além da própria criação.”

Mod **client-side** para **Minecraft 26.2** (Fabric) que reúne HUD de informações,
ajustes visuais e automações de conforto em um único painel, aberto no jogo com
**`/zeyafabric`**.

Tudo é local: nenhuma alteração visual é enviada ao servidor, e hitboxes, alcance
e atributos continuam exatamente os do vanilla.

| | |
|---|---|
| **Versão** | 1.0.2 |
| **Ambiente** | client-only |
| **Minecraft** | 26.2 |
| **Licença** | MIT |

## Recursos

### HUD e informações

- **FPS, ping e TPS** no painel de informações, com o mesmo layout compacto do
  HUD de referência. O TPS é estimado
  a partir dos pacotes de tempo do servidor e pode aparecer como `--` até haver
  amostras suficientes.
- **Ping** medido por pacotes de ping do protocolo (uma vez por segundo), com a
  latência da lista de jogadores como fallback.
- **Painéis arrastáveis**: `/zeyafabric hud` abre a tela de posicionamento;
  arraste os painéis e solte o mouse para salvar. A página dedicada de HUD
  reúne os toggles de FPS/Ping/TPS e Enemy Info, o editor de posição, os
  seletores de cor do HUD e a opção de exibir ou ocultar o fundo dos painéis.
  As configurações de DPS ficam em uma página própria, acessível pelo menu
  principal.
- **Combat HUD**: estima o DPS por quedas de vida e números de dano flutuantes
  recebidos pelo cliente, incluindo labels de `Text Display` e `Armor Stand`
  usados por servidores como o Hypixel. A distância é configurável de 1 a 32
  blocos (15 por padrão), sem exigir que o alvo esteja na tela ou que o jogador
  mire nele. O cálculo usa uma janela de 3 segundos e zera após 5 segundos sem
  dano; valores acima de mil são abreviados com `k`, `m`, `b` e `t`. O resumo de
  dano no chat é opcional, vem desligado por padrão e agrega os acertos em uma
  mensagem no máximo a cada 3 segundos.
- **Alvo e vida ao mirar**: nome, vida e barra de um jogador ou mob a até 64
  blocos, desde que não haja bloco sólido no caminho. Armor Stands usados como
  marcadores são ignorados, e o alcance vale só para o HUD — não para interação.
- **Leitor de vida** compatível com marcadores `HP`, `health`, `vida`, `life`,
  corações e pares no formato `atual/máximo` (ex.: `20M/20M`), inclusive quando o
  nome e a vida vêm na mesma linha do nametag. Também lê a boss bar do servidor
  quando o nome dela traz a vida, usando o progresso da barra para estimar o
  máximo, e o placar `below_name` quando o objetivo é de vida.
- **Nomes do servidor**: o HUD procura nametags/hologramas (Text Display, Armor
  Stand e entidades invisíveis com nome) num raio de 12 blocos na horizontal e
  32 na vertical, e usa o nome da boss bar quando o mob ainda aparece com o nome
  vanilla. O botão **Log de nomes/vida (debug)** no menu do Combat HUD grava no
  log os textos encontrados para diagnóstico.
- O HUD permite configurar separadamente as cores do texto, do fundo (incluindo
  transparência) e da barra de vida. O painel de fundo pode ser desligado na
  página de HUD. Enemy Info funciona independentemente do painel de DPS; o DPS
  continua configurável na página Combat HUD. O Combat HUD
  depende dos dados de vida visíveis ao cliente — servidores que os ocultam podem limitar o que aparece. Se o servidor
  desenhar a vida sem enviá-la como entidade ou nome visível, o HUD fica restrito
  aos atributos vanilla.

### Visual

- **Tamanho do personagem** de 0,1x a 1,5x, só na terceira pessoa:
  `/zeyafabric size 0.3` ou `/zeyafabric size reset`.
- **Perspectiva**: alterna entre primeira pessoa e a vista traseira em terceira
  pessoa (a vista frontal fica desativada).
- **Glow / contorno local** do jogador, com a cor global do mod.
- **Contorno das hitboxes**: desenha apenas as bordas das caixas de jogadores e
  entidades próximas. É visual — não altera as hitboxes nem os dados enviados.
- **Esconder armadura** e, separadamente, **apenas o item da cabeça** (uma cabeça
  de Steve ou Creeper, por exemplo), sem esconder a cabeça nem a skin.
- **Reduzir mobs maiores que o player**: aplica metade da escala visual a mobs
  mais altos ou mais largos que você. Não muda hitboxes, alcance ou atributos.
- **Fullbright**.
- **Cor global do mod**: ajuste RGB único aplicado às mensagens, HUDs, textos e
  botões dos menus e destaques visuais, incluindo Glow, hitboxes e marcadores
  de inventário. Os fundos dos painéis permanecem escuros para facilitar a leitura.

### Automação e conforto

- **Auto-click esquerdo** de 1 a 15 CPS, ativo somente enquanto o botão físico
  esquerdo estiver pressionado. O intervalo entre cliques é calculado diretamente
  a partir do CPS configurado.
- **Pesca automática**: `/zeyafabric fishing`. Começa após o primeiro lançamento
  manual da vara e continua recolhendo e lançando enquanto a opção estiver ativa.
  Tem atraso configurável após a fisgada e entre lançamentos, espera mínima do
  peixe, pausa ao abrir menus e nova tentativa se o recolhimento falhar por lag. Detecta
  a fisgada vanilla e marcadores como `!!!` perto do anzol. A opção **Auto
  Golden Fish** detecta os avisos do chat, localiza o peixe e ajusta a mira
  gradualmente para recolher/lançar durante o encontro; é opcional nas opções
  de macros.
- **Recuperação de oxigênio**: mantém você subindo apenas quando submerso e
  abaixo do limite configurado, restaurando o estado original da tecla de pulo
  ao parar.
- **Macros**: Blaze Dagger, troca de loadout, wardrobe com keybinds e troca
  rápida.
- **Slot bind / slot lock**: vincule teclas a slots e trave slots específicos.
- **Key commands**: dispare comandos rápidos por tecla.
- **Controle global**: suspende ou reativa todos os recursos de uma vez, sem
  apagar as configurações individuais.

Todos os comandos de alternância aceitam `on`, `off` e `toggle`:

```text
/zeyafabric fishing toggle
/zeyafabric size 0.5
/zeyafabric hud
```

## Organização do código

As classes das features ficam separadas por área em `hud/`, `visual/`,
`macros/` e `network/`. A inicialização, configurações e menu principal ficam
na raiz do pacote `dev.zeya.fabricmoon`, e os mixins permanecem em `mixin/`.
O menu principal agrupa as opções em quatro painéis — HUD, Visual, Macros e
Loadout — com disposição adaptável ao tamanho da janela. A categoria HUD abre
uma página dedicada aos controles visuais e mantém as opções de rede no painel
principal. Loadout mantém os
controles de Wardrobe/Equipment e a configuração de teclas rápidas; a interface
para salvar, carregar e remover loadouts foi retirada.

## Requisitos

- Minecraft 26.2
- Fabric Loader 0.20.1 ou superior
- Fabric API 0.161.0+26.2 ou superior
- Java 25

Mod Menu é opcional: com ele instalado, as configurações também aparecem no
botão de configuração do ZeyaMoon na lista de mods.

## Instalação

1. Pegue o jar mais recente em [`dist/`](dist/) (ou compile você mesmo, veja
   abaixo).
2. Coloque o `.jar` na pasta `mods/` do seu Minecraft.
3. Inicie o jogo com o perfil Fabric 26.2 e digite `/zeyafabric` no chat.

## Compilar

No Windows:

```powershell
gradlew.bat build
```

O JAR é gerado em `build/libs/`. Para atualizar a cópia versionada em `dist/`,
copie o jar novo para lá antes de commitar.

O ícone do mod fica em `src/main/resources/assets/zeyafabricmoon/icon.png`. Esse
arquivo é usado pelo Minecraft na lista de mods e pode ser enviado como imagem
do projeto no Modrinth.

## Aviso

Recursos de automação (auto-click, pesca automática, macros) podem violar as
regras de servidores multiplayer. **Verifique as regras do servidor antes de
usar.**
