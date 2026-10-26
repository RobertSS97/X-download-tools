# XDownload

App Android nativo (Kotlin + Jetpack Compose) para baixar vídeos e GIFs de posts do X (antigo Twitter), com a mesma linguagem visual do **RotaPRO**.

## Recursos

- **Compartilhar → XDownload**: no app do X, toque em *Compartilhar* no post e escolha *Baixar com XDownload*. O link é lido e o vídeo é buscado na hora.
- Colar o link manualmente (botão **Colar**) ou usar a sugestão do link copiado.
- Escolha de **qualidade** (1080p, 720p, 360p…) com resolução e tamanho estimado; posts com vários vídeos têm seletor.
- Download pelo `DownloadManager` do sistema: continua em segundo plano, mostra notificação e salva em **Filmes/XDownload** (aparece na galeria).
- **Histórico** com miniatura, duração e qualidade; abrir, compartilhar, abrir o post no X ou remover.
- **Ajustes**: tema Sistema/Claro/Escuro, texto grande, qualidade sugerida e sugestão do link copiado.

## Design (herdado do RotaPRO)

| Elemento | Valor |
| --- | --- |
| Primária | `#0B63F6` (claro) / `#6EA2FF` (escuro) |
| Fundo escuro | `#0A0F1A` |
| Estados | verde `#1FA463`, âmbar `#F59E0B`, vermelho `#E5484D` |
| Campos e botões | 60dp / 56dp de altura, cantos de 18dp, borda de 1,5dp que acende no foco |
| Painéis | vidro com blur do fundo em movimento (`glass`, `backdropBlur`), cantos de 24–28dp |
| Movimento | `pressable` (encolhe em mola + vibração), `reveal` (entrada escalonada) |
| Ícones | Material Icons *Rounded* |

O blur em tempo real usa `RenderEffect` (Android 12+). No Android 10 e 11 os painéis ficam com véu translúcido sem desfoque.

## Como o vídeo é encontrado

1. Endpoint público de incorporação de posts (`cdn.syndication.twimg.com/tweet-result`), o mesmo dos embeds em sites. Não exige login.
2. Se falhar (post sensível, limite de acesso), usa a API pública do [FxTwitter](https://github.com/FixTweet/FxTwitter).

Só os MP4 são oferecidos (o HLS `.m3u8` é descartado). Posts privados ou com restrição de idade podem não estar disponíveis.

## Estrutura

```
app/src/main/java/com/nodexati/xdownload/
├── MainActivity.kt              # tema, edge-to-edge, recebe links compartilhados
├── data/                        # XLink (links), XClient (extração), Downloader, HistoryStore
├── settings/AppSettings.kt      # preferências observáveis
└── ui/
    ├── AppRoot.kt               # fundo em movimento + barra de navegação de vidro
    ├── theme/Theme.kt           # XDownloadTheme (cores do RotaPRO)
    ├── components/              # glass/backdrop, pressable, reveal, botões, campo
    └── screens/                 # baixar, historico, ajustes
```

## Compilar

Requisitos: JDK 17+ e Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Cada push roda o workflow **Android** no GitHub Actions, que publica o APK de debug como artefato.

minSdk 29 (Android 10), targetSdk 35.

> Baixe apenas conteúdo que você tem direito de usar.
