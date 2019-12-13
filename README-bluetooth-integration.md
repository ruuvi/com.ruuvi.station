# Ruuvi Custom Bluetooth provider integration

## Interfaces to implement:

The `BluetoothScanningGateway` scans for bluetooth devices and produces `LeScanResult` or a `RuuviTag`

The `BluetoothRangeGateway` scans for bluetooth devices in range and produces a `List<RuuviTag>`

### Factories:

`LeScanResultFactory`

`BluetoothScanningGatewayFactory`

`BluetoothRangeGatewayFactory`

### Gateways:

`BluetoothScanningGateway`

`BluetoothRangeGateway`

### Models:

`LeScanResult`