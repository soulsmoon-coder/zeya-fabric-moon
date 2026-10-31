# ZeyaMoon — A Lua do Eclipse Primordial

> “Nascida antes da luz, despertada no eclipse e destinada a existir além da própria criação.”

Mod **client-side** para **Minecraft 26.2** (Fabric) que reúne HUD de informações,
ajustes visuais e automações de conforto em um único painel, aberto no jogo com
**`/zeyafabric`**.

Tudo é local: nenhuma alteração visual é enviada ao servidor, e hitboxes, alcance
e atributos continuam exatamente os do vanilla.

| | |
|---|---|
| **Versão** | 1.0.0 |
| **Ambiente** | client-only |
| **Minecraft** | 26.2 |
| **Licença** | MIT |

## Recursos

### HUD e informações

- **FPS, ping, CPS e TPS** no mesmo painel. O CPS mostra os cliques esquerdo e
  direito (manuais e automáticos) em uma janela de um segundo; o TPS é estimado
  a partir dos pacotes de tempo do servidor e pode aparecer como `--` até haver
  amostras suficientes.
- **Ping** medido por pacotes de ping do protocolo (uma vez por segundo), com a
  latência da lista de jogadores como fallback.
- **Painéis arrastáveis**: `/zeyafabric hud` abre a tela de posicionamento;
  arraste os painéis e solte o mouse para salvar.
- **Combat HUD**: dano observado nas entidades que você acerta e total acumulado
  durante o combate. Valores acima de mil são abreviados com `k`, `m`, `b` e `t`.
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
- O Combat HUD tem fundo transparente e depende dos dados de vida visíveis ao
  cliente — servidores que os ocultam podem limitar o que aparece. Se o servidor
  desenhar a vida sem enviá-la como entidade ou nome visível, o HUD fica restrito
  aos atributos vanilla.

### Visual

- **Tamanho do personagem** de 0,1x a 1,5x, só na terceira pessoa:
  `/zeyafabric size 0.3` ou `/zeyafabric size reset`.
- **Perspectiva**: alterna entre primeira pessoa e a vista traseira em terceira
  pessoa (a vista frontal fica desativada).
- **Visão por baixo**: inclina a câmera de terceira pessoa para baixo do
  personagem sem alterar a direção para onde ele olha.
- **Glow / contorno local** do jogador, com presets de cores nomeados e ajuste
  RGB para qualquer cor.
- **Contorno das hitboxes**: desenha apenas as bordas das caixas de jogadores e
  entidades próximas. É visual — não altera as hitboxes nem os dados enviados.
- **Esconder armadura** e, separadamente, **apenas o item da cabeça** (uma cabeça
  de Steve ou Creeper, por exemplo), sem esconder a cabeça nem a skin.
- **Reduzir mobs maiores que o player**: aplica metade da escala visual a mobs
  mais altos ou mais largos que você. Não muda hitboxes, alcance ou atributos.
- **Fullbright**.

### Automação e conforto

- **Auto-click** independente nos botões esquerdo e direito, de 1 a 15 CPS. Cada
  um só funciona enquanto o respectivo botão físico estiver pressionado.
- **Auto-click direito para colocar blocos**: `/zeyafabric rightclick`
  (também aceita `place` ou `block`).
- **Pesca automática**: `/zeyafabric fishing`. Recolhida com atraso aleatório de
  1 a 7 ticks, espera configurável entre lançamentos, espera mínima do peixe,
  pausa ao abrir menus e nova tentativa se o recolhimento falhar por lag. Detecta
  a fisgada vanilla e marcadores como `!!!` perto do anzol. Golden Fish e
  failsafe não estão incluídos.
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
/zeyafabric rightclick on
/zeyafabric fishing toggle
/zeyafabric size 0.5
/zeyafabric hud
```

## Requisitos

- Minecraft 26.2
- Fabric Loader 0.20.1 ou superior
- Fabric API 0.161.0+26.2 ou superior
- Java 25

Mod Menu é opcional: com ele instalado, as configurações também aparecem no
botão de configuração do ZeyaMoon na lista de mods.
