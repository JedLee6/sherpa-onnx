// Copyright (c)  2024  Xiaomi Corporation
import 'dart:io';
import 'dart:isolate';
import 'package:path/path.dart' as p;
import 'package:sherpa_onnx/sherpa_onnx.dart' as sherpa_onnx;

Future<void> initSherpaOnnx() async {
  String platform = '';

  if (Platform.isMacOS) {
    platform = 'macos';
  } else if (Platform.isLinux) {
    platform = 'linux';
  } else if (Platform.isWindows) {
    platform = 'windows';
  } else {
    throw UnsupportedError('Unknown platform: ${Platform.operatingSystem}');
  }

  var uri = await Isolate.resolvePackageUri(
      Uri.parse('package:sherpa_onnx_$platform/any_path_is_ok_here.dart'));

  if (uri == null) {
    print('File not found');
    exit(1);
  }

  var libPath = p.join(p.dirname(p.fromUri(uri)), '..', platform);
  if (platform == 'linux') {
    final arch = Platform.version.contains('arm64') ||
            Platform.version.contains('aarch64')
        ? 'aarch64'
        : 'x64';
    libPath = p.join(p.dirname(p.fromUri(uri)), '..', platform, arch);
  }

  sherpa_onnx.initBindings(libPath);
}
