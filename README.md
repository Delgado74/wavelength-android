# Wavelength Android

Wallet Android autocustodiada para [Wavelength](https://github.com/lightninglabs/wavelength), construida con Kotlin y Jetpack Compose. Integra el daemon embebido mediante el SDK gomobile oficial (`Wavewalletdk.aar`).

## Funciones

- Crear wallet nueva y mostrar la frase semilla para respaldo.
- Restaurar una wallet existente desde 24 palabras.
- Desbloquear con contraseña local.
- Balance confirmado y pendiente.
- Recibir por Lightning (factura) y por on-chain (dirección de boarding).
- Enviar pagos Lightning y on-chain con cotización previa de comisión.
- Historial de actividad en tiempo real con actualización periódica, origen
  (Lightning, Ark u on-chain) y confirmaciones de depósitos on-chain.
- Detalle de cada operación al tocarla: monto, comisión, estado, fase, txid,
  direcciones, payment hash, preimagen, VTXO y enlace a mempool.
- Cambio de red: signet, testnet4 y mainnet.

## Redes

| Red | Uso |
| --- | --- |
| `signet` | Predeterminada. Ideal para probar. |
| `testnet4` | Pruebas en testnet4. |
| `mainnet` | Fondos reales. Requiere habilitar `allow_mainnet` en el SDK. |

Los endpoints del operador Ark, del swap server y de Esplora se dejan a los valores por defecto del daemon (Lightning Labs):

| Red | Operador Ark | Swap server |
| --- | --- | --- |
| `signet` | `signet.wavelength.lightning.finance:443` | `swap.signet.wavelength.lightning.finance:443` |
| `testnet4` | `lumosd-testnet4.testnet.lightningcluster.com:443` | `swapd-testnet4.testnet.lightningcluster.com:443` |
| `mainnet` | `wavelength.lightning.finance:443` | `swap.wavelength.lightning.finance:443` |

> **Mutinynet no está soportado.** El daemon de Wavelength v0.2.0 solo acepta
> `mainnet`, `testnet`, `testnet4`, `signet`, `regtest` y `simnet`. Mutinynet usa
> parámetros de cadena propios, por lo que no basta con apuntar el operador de
> Mutinynet usando `network=signet`.

## Compilación

La compilación se hace en GitHub Actions; no hace falta descargar el SDK ni el Android SDK localmente.

1. El workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) descarga `Wavewalletdk.aar` de la release `v0.2.0`, lo cachea y ejecuta `./gradlew :app:assembleDebug`.
2. Al terminar, el APK queda como artefacto `wavelength-android-debug` en la pestaña **Actions** del repositorio.
3. El APK es solo `arm64-v8a` (dispositivos Android de 64 bits modernos).

Para compilar en un equipo con el SDK descargado:

```bash
./scripts/fetch-aar.sh
./gradlew :app:assembleDebug
```

El AAR no se versiona en el repositorio (ver `.gitignore`). La clave
`keystore/debug.keystore` sí se versiona a propósito: solo firma builds de
depuración y así las actualizaciones se instalan encima sin cambiar de firma.

## Estructura

```
app/src/main/java/io/github/delgado74/wavelength/
├── MainActivity.kt
├── WalletViewModel.kt
├── walletkit/          # Puente Kotlin sobre el SDK gomobile
│   ├── Models.kt
│   ├── WalletClient.kt
│   └── WalletConfig.kt
└── ui/                 # Pantallas Compose
    ├── AppRoot.kt
    └── Theme.kt
```

## Seguridad

- La frase semilla se muestra una sola vez y nunca se envía a ningún servidor.
- La contraseña cifra la wallet en el dispositivo y no se almacena.
- No se permite tráfico HTTP en texto plano.
- `allowBackup` está desactivado para evitar copias automáticas de datos sensibles.

Esta app es experimental. No uses fondos que no puedas permitirte perder.

## Licencia

MIT. Consulta [LICENSE](LICENSE) y [NOTICE](NOTICE).
