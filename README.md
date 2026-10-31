# ZeyaMoon — A Lua do Eclipse Primordial

> “Nascida antes da luz, despertada no eclipse e destinada a existir além da própria criação.”

Mod client-side para Minecraft 26.2 com configuracoes visuais locais.

O ícone do mod está em
`src/main/resources/assets/zeyafabricmoon/icon.png`. Esse arquivo é usado pelo
Minecraft na lista de mods e pode ser enviado como imagem do projeto no Modrinth.

## Requisitos

- Minecraft 26.2
- Fabric Loader 0.20.1 ou superior
- Fabric API 0.161.0+26.2 ou superior
- Java 25

## Compilar

No Windows, execute `gradlew.bat build`. O JAR sera criado em `build/libs/`.

Dentro do jogo, digite **`/zeyafabric`** no chat para abrir o painel.
O submenu **Controle global** permite suspender ou reativar todos os recursos
do mod sem apagar as configuracoes individuais.
Use **`/zeyafabric hud`** para abrir a tela de posicionamento; arraste os paineis
de estatisticas e Combat HUD para reposiciona-los e solte o mouse para salvar.
Use **`/zeyafabric rightclick`** (ou `/zeyafabric place` ou `/zeyafabric block`) para alternar o
auto-click direito usado ao colocar blocos. Os comandos `... rightclick on`,
`... rightclick off` e `... rightclick toggle` tambem estao disponiveis.
Use **`/zeyafabric fishing`** para alternar a pesca automatica, ou acrescente
`on`, `off` ou `toggle` para escolher o estado explicitamente. Esses comandos
locais tambem sao interceptados pelo evento de envio de comandos do Fabric.
Use **`/zeyafabric size 0.3`** para definir o tamanho visual do personagem entre
0.1x e 1.5x; **`/zeyafabric size reset`** restaura 1.0x. O tamanho tambem pode
ser ajustado no submenu **Player Size**, acessivel pelo painel principal.
O mesmo submenu mostra a perspectiva atual em uma unica opcao; use F5 para
alternar entre primeira e terceira pessoa. O botao dessa opcao tambem alterna a
camera. O tamanho configurado afeta apenas o personagem em terceira pessoa.
O submenu principal tambem abre as opcoes de glow/contorno local do jogador,
com presets de cores nomeados em ingles e ajuste RGB para escolher qualquer cor.
O submenu tambem oferece a opcao "Visao por baixo", que inclina a camera de
terceira pessoa para baixo do personagem sem alterar a direcao para onde ele olha.
O painel principal tambem tem botoes diretos para ativar ou desativar a pesca
automatica e esconder ou mostrar a armadura; as opcoes detalhadas da pesca ficam
no submenu Survival.
Com o Mod Menu instalado, as configuracoes tambem aparecem no botao de
configuracao do ZeyaMoon na lista de mods.

O painel permite esconder sua armadura e, separadamente, ocultar apenas o item
equipado na cabeca (por exemplo, uma cabeca de Steve ou Creeper), sem esconder
a cabeca nem a skin do jogador. O submenu **Player Size** permite ajustar o
tamanho visual entre 0,1x e 1,5x, inclusive deixar o personagem bem pequeno.
A escala afeta apenas a terceira pessoa. A tecla de perspectiva alterna entre
primeira pessoa e a vista traseira
em terceira pessoa; a vista frontal fica desativada. As alteracoes visuais nao
alteram hitboxes, dados enviados ao servidor ou a aparencia vista por outros jogadores.

O HUD opcional mostra FPS, ping, CPS dos botoes esquerdo e direito (cliques
manuais e automaticos em um intervalo de um segundo) e uma estimativa de TPS calculada
a partir dos pacotes de tempo recebidos do servidor. O ping e medido por solicitacoes
de ping do protocolo enviadas uma vez por segundo, com a latencia da lista de jogadores
como fallback. O TPS pode aparecer como `--` ate que o servidor envie amostras suficientes.
O botao **Contorno das hitboxes** desenha apenas as bordas das caixas de jogadores e
entidades proximas; o recurso e local e nao altera as hitboxes nem os dados enviados
ao servidor.
Em **Player Size**, **Reduzir mobs maiores que o player** aplica metade da escala
visual a mobs mais altos ou mais largos que o jogador local. Isso nao muda hitboxes,
alcance, atributos ou dados enviados ao servidor.
O Combat HUD opcional mostra dano observado em entidades atingidas pelo jogador,
o total acumulado durante o combate. Esses valores dependem dos dados de vida visiveis ao cliente e podem
nao corresponder ao dano personalizado exibido por alguns servidores. Abra o
submenu **Combat HUD** no painel principal para ativar o recurso, escolher quais
informacoes mostrar e resetar o dano acumulado; ele nao automatiza ataques. A opcao
**Mostrar alvo e vida (mirando)** pode ser desativada separadamente e exibe o nome,
a vida e uma barra ao mirar em um jogador ou mob a ate 64 blocos, sem blocos solidos
no caminho. O alvo direto segue a mira normal do jogo; a busca ampliada ignora
Armor Stands usados como marcadores. Esse alcance so afeta o HUD, nao o alcance
de interacao. O painel de dano aparece depois de um dano observado e some quando o total expira; o HUD do
alvo aparece enquanto voce mira numa entidade. O leitor de vida aceita marcadores
como `HP`, `health`, `vida`, `life`, coracoes e pares no formato `atual/maximo`
(por exemplo, `20M/20M`). Ele tambem le a boss bar do servidor quando o nome dela
traz a vida, usando o progresso da barra para estimar o maximo quando necessario.
O fundo do Combat HUD e transparente; servidores que ocultam
esses dados podem limitar o valor exibido.
Valores de dano e vida acima de mil sao abreviados com `k`, `m`, `b` e `t`.
O leitor tambem procura o texto de vida proximo ao mob (como etiquetas flutuantes
do servidor); se o servidor desenhar a vida sem enviar esse texto como entidade
ou nome visivel ao cliente, o HUD continua limitado aos atributos vanilla.

O painel tambem oferece um modo Fullbright e auto-click independente para os
botoes esquerdo e direito. Cada auto-click so funciona enquanto o respectivo
botao fisico estiver pressionado, com velocidade configuravel entre 1 e 15 CPS.

As opcoes Survival incluem pesca automatica comum com recolhida em atraso
aleatorio de 1 a 7 ticks, espera configuravel entre lancamentos, espera
configuravel em segundos depois de lancar a vara antes de aceitar uma fisgada,
espera minima do peixe e pausa ao abrir menus. Quando a espera termina, a vara e
recolhida ao detectar a fisgada. Se o recolhimento falhar por lag, o mod tenta
novamente em vez de ficar travado. A deteccao considera a fisgada vanilla e
marcadores de pesca como `!!!` perto do anzol.
A recuperacao de oxigenio mantem o jogador subindo apenas quando esta submerso
e abaixo do limite configurado, restaurando o estado original da tecla de pulo
ao parar. Golden
Fish e failsafe nao estao incluidos. Verifique as regras do servidor antes de
usar recursos de automacao em multiplayer.
