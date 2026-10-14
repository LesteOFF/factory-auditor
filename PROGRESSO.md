# PROGRESSO — Auditor de Fábrica (addon da Central)

Plano: `future-mods/factory-auditor-integration.md`

## Estado
- Fase atual: todas concluídas
- Concluídas: 0, 1, 2, 3, 4, 5 (`gradlew --offline -q build test runGameTestServer` verde: 53 JUnit, 12 gametests; Central verde com 22 gametests depois de abrir a API)
- Versão atual: **1.5.0** (`gradle.properties`), exige a Central `[2.1.0,3)`. O jar `monitoringcenter_auditor-1.5.0.jar` no pack é anterior à correção de 14/10/2026 (entrada com 4096 slots, abaixo); o `build/libs/monitoringcenter_auditor-1.5.0.jar` já tem a correção, falta copiá-lo para o pack e para o servidor.
- Revisão mcdev da fase 1: aplicada na fase 2 (handler por face que confere o lado, `setChanged` 1x/tick, sync após mudança de status, invalidar capacidades só se E/S ou modo mudam, update tag enxuta sem dono/bandeja, janela suavizada semeada no load, GUI acompanha o servidor e fecha se o bloco some, `versionRange` da Central `[2.0.0,3)`, `ftbteams` fora do mods.toml).
- Revisão mcdev da fase 2: aplicada na fase 3 (nova corrida recusada com outra em andamento, GUI reconstrói quando a corrida começa/termina, contagem sincronizada a cada 10 s e interpolada no cliente, cancelamento avisa o dono, metas lidas em ordem fixa).
- Revisão mcdev da fase 3: aplicada na fase 4 (rebuild não carrega chunk: `shouldTickBlocksAt` antes de ler a Placa, e Placas paradas não gastam a cota do dono; tick extra rechecado contra a tag e o tipo, com o estado atual do mundo e `try/catch` que tira a máquina da lista; rebuild por mudança no máximo a cada 20 ticks; sem aceleração em dimensão sem jogadores nem chunks forçados; estado limpo no unload da dimensão; royalties em pilhas do tamanho máximo do item; `moving_piston`, `sculk_shrieker` e `sculk_catalyst` na tag).
- Revisão mcdev da fase 4: aplicada na fase 5 (empate não toma recorde e o recordista fica gravado, Tab mantém o prefixo do time do scoreboard, anúncio só quando alguém é superado, itens removidos do jogo saem do placar ao carregar, nomes cortados em 32 no pacote, item da aba Placar resolvido uma vez).
- Revisão final (fase 5 + addon inteiro): aplicada (marcos guardados pelo maior limiar entregue, então um datapack que adiciona ou remove marcos não repete nem pula prêmio; `minecraft:air` recusado como prêmio e pilha vazia ignorada na bandeja; bandeja sem limite; chave de lang do Jade; hitbox do troféu cobre as alças). Não aplicado: selos repetidos (mesmo item e nível) contam para títulos e marcos.
- Arte final pelo Blockbench (MCP): texturas do Auditor (lado, topo, frente com vidro), Placa e borda, selo (tons de cinza para a cor do nível), três diplomas, dois troféus (modelo exportado pelo Blockbench) e a folha `font/medals.png`.
- Fase 3: orçamento "spark com dez Placas" não medido (exige o jogo). Guarda: tetos `maxPlaquesPerOwner`/`maxMachinesPerPlaque`, lista refeita a cada 100 ticks, `shouldTickBlocksAt` antes de cada tick extra.

## Fase 0
- Central: `WorkshopScreen` virou `public abstract` (construtor e cores `INK`/`MUTED`/`ACCENT` protegidos); `Scenario` público (classe, construtor, constantes e métodos). Central verde: 22 gametests + JUnit.
- Perguntas da fase 0, respondidas pelo run `runPackGameTestServer` (mods reais do pack, `gametest/IntegrationGameTests`):
  - Create (`create:chute`): 64 de 64 chegam ao baú e o total bate; com destino cheio o chute segura os 16 itens, nada some.
  - Mekanism (`mekanism:basic_logistical_transporter`): 64 de 64 e total bate; com destino cheio o transportador recusa os itens já na entrada (0 aceitos), nada some.
  - Oritech (`oritech:item_pipe`, lado do baú em modo extrair): 64 de 64 e total bate.
  - Consumir aguenta 218 itens/tick (gametest `consumeTakesTwoHundredEighteenItemsEveryTick`, sem mod): total 21 800 em 100 ticks, taxa 261 600/min.
  - AE2 (export bus): só no jogo — exige colocar parte num cabo e montar rede ME com energia e armazenamento; não vale um teste instável.
  - Custo da aceleração no spark: continua só no jogo.
  - Conclusão: nenhum mod testado ignora a recusa, então o buffer previsto no plano não é necessário.

## Decisões que mudaram algo do plano
- Ruling: esqueleto Gradle copiado da Central (mesma linhagem do nostalgia-skills, mesmas versões e o padrão `libs/` exigido pelo plano) — custo se errado: nenhum, os arquivos são equivalentes.
- Ruling: o código da fase 0 (bloco de passagem) não é descartado, vira o início da fase 1 — evita escrever o mesmo bloco duas vezes.
- Ruling: a métrica `audit_rate` é registrada no `FMLCommonSetupEvent` (`enqueueWork`), não no construtor — os construtores dos mods rodam em paralelo e `Metrics` usa `LinkedHashMap` sem trava.
- Ruling: em Dividir, a sobra indivisível vai a uma saída em rodízio, não sorteada — inserções de 1 item (funil) ficam exatamente iguais (64/64); custo se errado: nenhum visível.
- Ruling: JUnit roda com as classes do Minecraft no classpath (`addModdingDependenciesTo(sourceSets.test)`), para testar `IoConfig` com `Direction`.
- Ruling: GUI abre por pacote `OpenAuditor` do servidor (igual ao Sensor da Central) e lê o block entity do cliente, sincronizado pela update tag; o Jade usa só esse estado do cliente (sem `IServerDataProvider`).
- Ruling: `versionRange` da Central `[2.0.0,3)` em vez de `[2.0.0,)` — o addon herda a API protegida de `WorkshopScreen`; uma 3.x pode quebrar a GUI. Custo se errado: subir o teto quando a Central for para 3.
- Ruling: o item da certificação é o da mão no momento de `StartCertification` (como `SetSensorFilter`); a meta base vai no `OpenAuditor` para a GUI mostrar a meta do nível. Só conta o item auditado; outros itens são consumidos sem contar.
- Ruling: metas `audit_goals/*.json` com `item` OU `tag` OU nenhum (o arquivo sem os dois é a meta padrão, `default.json`). A checagem "aponta para item válido" é gametest (`everyAuditGoalPointsToAValidItem`, registros vivos); o JUnit confere formato e ids.
- Ruling: bandeja sem limite (selos e prêmios únicos nunca caem no chão para sumir). Shift+clique com a mão vazia retira a bandeja sem abrir a GUI.
- Ruling: revisão da fase 1, não aplicados: alocação em `outlets()` por inserção (custo baixo, otimizar se o spark mostrar), ponte por `Dist` para `ClientHandlers` (a Central usa o mesmo padrão), dono = quem coloca (FakePlayer vira dono; dispenser deixa sem dono e qualquer um configura).
- Ruling: revisão da fase 2, não aplicados: Shift+clique na bandeja só funciona com as duas mãos vazias (regra do vanilla para uso secundário); corrida pausa com o chunk descarregado e retoma ao carregar (sem perda nem reprovação); qualquer nível pode ser pedido sem ter o anterior (o plano não exige escada).
- Ruling: aceleração = acumulador por máquina (`progress += bônus`, um tick extra a cada 1,0). Raio em cubo (`plaqueRadius`), máquinas mais próximas primeiro até `maxMachinesPerPlaque`. Fora: tag `not_accelerated` (spawner, trial_spawner, vault, beacon, conduit, end_gateway) e os namespaces da Central e do addon.
- Ruling: royalties = taxa certificada do selo × `royaltyPercent`, pagos a cada segundo no inventário atrás da Placa; recusa fica devendo até 64. Só o time dono do selo pendura ou retira.
- Ruling: os gametests de aceleração rodam em batches próprios (`acceleration_on`/`acceleration_off`) porque trocam o config global.
- Ruling: revisão da fase 3, não aplicados: aceleração vale para tudo fora da lista negra (o plano pede tag de exclusão, não lista de permitidos); royalties não têm teto por dono (cada selo vem de uma certificação própria).
- Ruling: placar em `board/Standings` (puro, testado) dentro do `SavedData` `Leaderboard`. Recorde = melhor taxa certificada do item; anúncio só quando o recordista muda. Títulos por selos (1 Auditado, 5 Fornecedor, 15 Industrial, 40 Magnata) e Monopolista com 5 recordes (anunciado ao conquistar). Tab: medalha do melhor nível (fonte `monitoringcenter_auditor:medals`), `xN` selos e título.
- Ruling: a aba Placar mostra o placar do item auditado (o da corrida ou o mais frequente) tirado na abertura da GUI (`BoardSnapshot` no `OpenAuditor`), sem atualização ao vivo.
- Ruling: `nextSealGoal` (capacidade da métrica `audit_rate`) = meta do próximo nível que o dono ainda não tem para o item mais frequente; durante a corrida, a meta da corrida.
- Ruling: revisão da fase 4, não aplicados: todo selo conta para títulos e marcos, mesmo repetido (o plano conta selos por dono); sem comando de op para apagar entradas; nome do dono = quem certificou por último (com FTB Teams o dono é o time).
- Ruling: fase 5 = cinco marcos por quantidade de selos lidos de `data/monitoringcenter_auditor/diplomas/*.json` (`seals` + `item`): 1 Diploma Júnior, 5 Troféu de Bronze, 15 Diploma Sênior, 40 Troféu de Ouro, 100 Diploma de Auditor-Mor. Entregues uma vez por dono, na bandeja do Auditor que emitiu o selo. Diplomas vão no slot `charm` do Curios só por dados (tag + atribuição do slot ao jogador), sem efeito em jogo; os troféus são os blocos de decoração.
- Ruling: a fonte das medalhas (56×8, 7 glifos U+E000–U+E006) também foi desenhada no Blockbench, por script de canvas.
- Ruling: `packJars` só com Jade; Curios entra só por dados (tag + slot) e FTB Teams já é resolvido pela Central via `Owners` — o addon não compila contra eles.

## Assinaturas conferidas (javap, NeoForge 21.1.256)
- `BlockCapabilityCache.create(BlockCapability, ServerLevel, BlockPos, C, BooleanSupplier, Runnable)`; `getCapability()`
- `IItemHandler`: `getSlots/getStackInSlot/insertItem(int, ItemStack, boolean)/extractItem/getSlotLimit/isItemValid`; `ItemHandlerHelper.insertItem(IItemHandler, ItemStack, boolean)`
- `PlayerEvent.TabListNameFormat.getDisplayName()/setDisplayName(Component)`; `ServerPlayer.refreshTabListName()`
- `Font.drawInBatch(String, float, float, int, boolean, Matrix4f, MultiBufferSource, Font.DisplayMode, int, int)`; `BlockEntityRenderer.getViewDistance()`
- `ItemRenderer.renderStatic(ItemStack, ItemDisplayContext, int, int, PoseStack, MultiBufferSource, Level, int)`
- `SimpleJsonResourceReloadListener(Gson, String)`; `AddReloadListenerEvent.addListener(PreparableReloadListener)`
- `ServerChunkCache.getChunkNow(int, int): LevelChunk`; `LevelChunk.getBlockEntities(): Map<BlockPos, BlockEntity>`
- `Inventory.placeItemBackInInventory(ItemStack)`; `PlayerList.broadcastSystemMessage(Component, boolean)`
- `SavedData.Factory(Supplier, BiFunction<CompoundTag, HolderLookup.Provider, T>)`; `DimensionDataStorage.computeIfAbsent(Factory, String)`
- `BlockStateBase.getTicker(Level, BlockEntityType<T>)`; `ILevelExtension.invalidateCapabilities(BlockPos)`
- `RegisterColorHandlersEvent.Item.register(ItemColor, ItemLike...)`

## Melhorias copiadas da Central (plano `fa-a-um-plano-glimmering-book.md`)
- Avisos offline: `board/Notices` manda a membros online e grava recado na caixa da Central (`MonitorNetwork.inbox()`, entregue no login) para quem está offline. Usado em certificação aprovada/reprovada/cancelada e marcos. Gametest: o dono offline recebe recado.
- Ruling: a caixa é a da Central (fila de 50 recados por jogador, dividida com os alarmes; formato JSON interno dela). Custo se errado: rajada de alarmes pode empurrar um recado do Auditor; se a Central mudar o formato, trocar por uma API pública dela.
- Limite de pacotes: `network/RateLimit` copiado da Central (40 pacotes/s por jogador), todos os `playToServer` passam por `ServerHandlers.limited`; esquecido no logout. `RateLimitTest` copiado.
- Run do pack: source set `packTest` e `runPackGameTestServer` (mundo em `run-pack/`), com Create, Mekanism, AE2, Oritech, Jade, Curios, FTB Teams e dependências. A Central agora fica em `libs/central/`.
- Correção achada pelo teste do Oritech: quando E/S ou modo mudam, o Auditor atualiza a forma e avisa os vizinhos; antes, um cano do Oritech colocado antes de configurar a face ficava desconectado.
- Teste do Tab usa `FakePlayer` (o jogador simulado quebrava com o Curios no run do pack).
- Create Display Link: `compat/create/AuditorDisplaySource` (uma linha: número + unidade, ou status, ou `Certificando m:ss`), registrada em `CreateRegistries.DISPLAY_SOURCE` como `auditor` e ligada ao bloco por `DisplaySource.BY_BLOCK.add`; carregada por `compat/Integrations` só com o Create presente. O texto do display saiu do renderer para `block/AuditorDisplayText` (comum), e `AuditorTexts` foi para o pacote raiz. Create entra como `compileOnly` (jars aninhados em `libs/create/`, extraídos com `unzip -o -j create-1.21.1-6.0.10.jar 'META-INF/jarjar/*.jar' -d libs/create`) e como dependência opcional no mods.toml. Gametest do pack confere o registro (e falha sem ele).
- Só no jogo: Display Link apontado para o Auditor escreve a taxa num Display Board.
- Revisões das três seções aplicadas: testes de destino cheio agora exigem nada no chão, o chute segurando o que recebeu, e depois de esvaziar o baú os 16 itens chegam (prova que o conector estava ligado); vizinhos atualizados com `UPDATE_CLIENTS` (sem cascata); bloco girado no lugar invalida as capacidades; o build só exige os jars de compilação (Jade, Create) na configuração e os do pack só ao rodar `runPackGameTestServer`; o jar da Central tem nome fixo (`central_jar` em gradle.properties) e é conferido antes de compilar.
- Ruling: não aplicados — constante `EXTRACT = 2` do Oritech no teste (conferida com javap); 218 itens/tick fixa a pergunta do plano, não um limite; o caminho de `refreshTabListName` após o selo fica sem gametest (o `FakePlayer` não está na lista de jogadores e o jogador simulado quebra com o Curios); Create/Flywheel/Ponder/Registrate ficam no classpath de compilação do `main` como na Central, com a regra de só importá-los em `compat/create`; os gametests continuam em `main` (vão no jar, como na Central).

## Correções do primeiro teste no jogo (14/10/2026)
- GUI não abria: `IllegalAccessError` (AuditorScreen → WorkshopScreen) porque o pack tinha um `monitoringcenter-2.0.0.jar` de antes de a API ser aberta, com o mesmo número de versão. Central subiu para **2.1.0** (regra do plano: abrir API sobe a versão menor) e o addon exige `[2.1.0,3)`; uma Central antiga agora é recusada na carga com mensagem clara. `central_jar=monitoringcenter-2.1.0.jar`.
- Cabos do Mekanism: a face de Saída passa a expor um inventário fechado (`block/ClosedFace`: não aceita nem entrega nada) para transportadores e canos se ligarem; Entrada continua aceitando e Desligada continua sem inventário. Gametests: `outputFacesOfferAClosedInventoryForCablesToConnect` e, no pack, `mekanismTransporterCarriesItemsAwayFromAnOutputFace`.
- O "Universal Cable" do Mekanism é de energia; o Auditor não usa energia, então ele não conecta (é o esperado).

## Versão 1.1 (plano `fa-a-um-plano-glimmering-book.md`, pedidos do Eduardo após o primeiro teste)
- Etapa 1 — só **Passar** e **Consumir**: `TransferMode {PASS, CONSUME}`; Passar reparte igualmente entre as saídas; prioridade removida de `IoConfig` (layout de bits mantido). Mundos antigos: `transfer` int 0/1 → Passar, 2 → Consumir; agora salvo pelo nome (`AuditorSettingsTest`). Protocolo de rede subiu para `"2"` (cliente antigo é recusado na conexão). Ruling: sem caminho de volta — mundo salvo na 1.1 aberto na 1.0 lê Consumir como Passar; fazer backup antes de voltar de versão.
- Etapa 2 — **Royalties removidos** (`RoyaltyMeter`, pagamento da Placa, `royaltyPercent`, campo `royalties` das metas). No lugar, **economia de energia**: nas máquinas aceleradas por uma Placa, o consumo de FE é medido em cada tick extra (`award/EnergyRefund`, média com a medição anterior) e a cada 20 ticks a máquina recebe de volta 5% (Cobre) a 35% (Estelar) desse consumo. Config `energySaving` (depende de `acceleration`).
  - Ruling: só máquinas da tag `monitoringcenter_auditor:energy_saving` recebem reembolso (máquinas do Mekanism e do Oritech já incluídas, entradas opcionais; extensível por datapack). Motivo: a capacidade de FE do Mekanism diz que permite extrair até nas máquinas, então não dá para separar máquina de bateria/gerador pela capacidade, e reembolsar bateria geraria energia infinita. Custo se errado: máquina de outro mod fora da tag não economiza até ser adicionada.
  - Gametest do pack: pulverizador do Oritech acelerado recebe reembolso; trocado por bateria (`oritech:small_storage_block`) depois do primeiro reembolso, não recebe mais; cubo do Mekanism (fora da tag) nunca recebe. O Mekanism colocado por `setBlock` no gametest não expõe energia nem itens pelos lados (a configuração de lados só se forma com o jogador colocando), então o teste usa o Oritech.
  - Revisão aplicada: tag e chunk ticando conferidos na hora de reembolsar (troca de bloco não vira energia grátis); reembolso só de janelas com tick extra medido, sem extrapolar média (máquina parada ou chunk parado não recebe); lado de energia escolhido entre os lados reais (o lado `null` do Mekanism é só leitura); `oritech:charger_block` e `oritech:laser_arm_block` fora da tag (só repassam energia).
- Etapa 3 — **Diplomas e troféus**:
  - Diploma no inventário ou no slot de amuleto do Curios (`compat/curios/CuriosCompat`, `CuriosApi.getCuriosInventory(...).isEquipped(Item)`). Vantagens na certificação (`diploma/Perks`, verificadas em quem clica Certificar): Júnior perdoa 1 minuto abaixo da meta; Sênior encurta a corrida em 40% (5 → 3 min); Auditor-Mor emite selo **De Excelência** (`SealData.excellent`, brilho; na Placa +2 de raio e +10 pontos de economia).
  - Ícone do maior diploma ao lado do nome no Tab e no chat (`PlayerEvent.NameFormat`), glifos U+E007–E009 na fonte `medals` (desenhados no Blockbench). Rechecado no login e a cada 30 s.
  - Troféus: clique dá Pressa I (Bronze) ou Pressa II + Velocidade I (Ouro) por 5 min a quem clicou e ao time online, recarga de 1 dia por jogador; luz 10/15; holograma com o melhor recorde do dono; fanfarra (som de conquista + partículas) quando o dono toma um recorde ou ganha um marco.
  - Ruling: o holograma mostra a melhor taxa certificada do dono (não precisa ser recorde do servidor).
  - Revisão aplicada: só o time dono usa o troféu (sem dono, ninguém); a recarga de 1 dia é **por troféu** e fica salva nele (por jogador o time revezava e a morte zerava); holograma recalculado ao carregar o chunk e com caixa de render maior; o minuto perdoado fica fora da taxa do selo e uma corrida de 1 minuto não pode ser perdoada; FakePlayer fora do cache de diplomas; glifo sem a cor do time. Não aplicado: duas fanfarras quando o mesmo selo bate recorde e marco.
- Etapa 4 — **GUI nova**, toda em sprites como a Central (`textures/gui/sprites/auditor/`, nine-slice no mesmo estilo dos sprites `workshop` da Central, desenhados no Blockbench; nada de `fill`):
  - Tela principal em três abas: **Visão geral** (taxa grande com o ícone do item, total, status com o motivo — "falta uma Entrada", etc. —, dois botões grandes Passar/Consumir, resumo das faces + botão **Faces…**, mini gráfico, Zerar total e Bandeja), **Certificar** (passo a passo com ícones de feito/pendente, 7 medalhas para escolher o nível, meta, vantagens dos seus diplomas, botão Certificar; durante a corrida, barra do minuto atual, tempo restante e perdões restantes) e **Placar**.
  - **Faces…**: cruz igual ao "Items Config" do Mekanism (Cima; Esquerda · Frente · Direita; Trás · Baixo), cada quadrado mostra o bloco vizinho, moldura azul/laranja/cinza por modo, legenda, Limpar e Voltar.
  - **Avançado…**: medição Instantânea/Suavizada, intervalo, tolerância e limite com explicação; campo inválido ganha moldura vermelha (sprite `field_invalid`).
  - `OpenAuditor` leva também os diplomas do jogador e a duração da corrida já com as vantagens.
- Revisão da GUI aplicada: Avançado…, Zerar total e Bandeja no rodapé (como a Central) e gráfico ajustado à altura, para caber em telas baixas; "Usar Consumir" dentro da área; botão Certificar desliga após o clique; item da corrida lido com `tryParse`; folga no topo do gráfico para o rótulo; modo já ativo não reenvia pacote.
- Só no jogo (1.1): tela de Faces com os ícones dos vizinhos; GUI legível na escala usada (sprites novos); medalhas nos botões de nível; holograma e fanfarra dos troféus; ícone de diploma no Tab e no chat; economia visível no consumo de uma máquina do Mekanism/Oritech perto de uma Placa (máquinas do Mekanism colocadas por jogador expõem energia pelos lados configurados).
- Versão do addon: **1.1.0** (`build/libs/monitoringcenter_auditor-1.1.0.jar`). Verificação: 69 JUnit, 18 gametests normais, 26 no run do pack.

## Correção de 14/10/2026: teto de ~15 mil itens/min com canos do Mekanism
- Causa: o transportador do Mekanism (`TransporterManager.getPredictedInsert`) simula o destino com `getSlots()` × `min(getSlotLimit, getMaxStackSize)` e desconta os itens já a caminho. Com 1 slot de 64, nunca havia mais de 64 itens em trânsito para o Auditor: 64 itens a cada ~5 ticks de viagem ≈ 15 360/min.
- Correção: `AuditorInput.SLOTS = 4096` (o slot é ignorado, todos levam ao mesmo destino), cerca de 262 mil itens em trânsito. Se o Auditor recusar algo num tick, os slots além do 0 recusam na hora até o tick seguinte (`AuditorBlockEntity.refusedThisTick`), para canos de outros mods não testarem os 4096 slots com o destino cheio.
- Gametest `everyInputSlotTakesAFullStackSoTransportersCanKeepThousandsOfItemsInFlight`. Verificação: `build test runGameTestServer` verde (19 gametests) e `runPackGameTestServer` verde (27).
- Falta conferir no jogo com os canos do Evolved Mekanism (mais de 100 mil itens por tick).

## Pendente fora do projeto
- `kubejs/server_scripts/factory_auditor/` no pack não foi criado (não mexo na instância). Metas específicas de itens de outros mods entram como datapack em `data/<qualquer>/audit_goals/*.json` (`item` ou `tag`, `base` em itens/min, `royalties` opcional).

## Integrações puladas
- Nenhuma: Jade, Curios e FTB Teams estão em `mods/`.

## Só dá para conferir dentro do jogo
- Perguntas da fase 0 (acima): funil do Create, tubo do Mekanism, export bus do AE2 e cano do Oritech respeitam a recusa e a contagem bate com o baú; Consumir aguenta 218 itens/tick.
- Display do Auditor legível nas 6 direções e no escuro, texto não espelhado; ícone do item mais frequente (renderizado como GUI, pode precisar girar ou acender) e o mesmo para o selo na Placa.
- GUI: cabe na escala usada; grade de E/S com clique esquerdo/direito e alternar faces na tela Faces…; campos com borda vermelha; abas Gráfico e Placar.
- Certificar um selo (5 min por padrão), tirar da bandeja (botão ou Shift+clique com as mãos vazias), pendurar na Placa, ver royalties no baú atrás e a aceleração nas máquinas; Jade mostra taxa, status e modo.
- Disputa de recorde entre dois times: anúncio, medalha, `xN` e título no Tab; `/auditor top <item>`.
- Sensor da Central encostado no Auditor lista `audit_rate` e a Tela mostra gráfico e alarme dela.
- Diplomas no slot de amuleto do Curios; troféus colocados no chão.
- Spark com dez Placas ativas e dez Auditores à vista (orçamento de desempenho da fase 3, não medido).
- Tirar o addon do servidor e confirmar que a Central abre o mundo sem erro.
