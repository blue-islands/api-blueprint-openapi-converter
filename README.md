# API Blueprint → OpenAPI Converter

API Blueprint (`.apib`) を OpenAPI 3.1 YAML に変換する Java / Maven プロジェクトです。

## 対応している主な記法

- `FORMAT: 1A`
- `HOST: https://...`
- `# タイトル`
- `## Resource [/path{?query}]`
- `### Action [GET]`
- `### Action [GET /path]`
- `+ Parameters`
- `+ Request`
- `+ Headers`
- `+ Response 200 (application/json)`
- JSON のレスポンス例からの簡易 schema 推論
- 日本語の説明文

## 想定ユースケース

特に、次のような API Blueprint を OpenAPI 化する用途を想定しています。

- 和座標 API
- GET 中心の API ドキュメント
- Header ベースの Bearer Token 認証説明を含む文書
- エラーサンプルを含む文書

## ビルド

```bash
mvn clean package
```

生成物:

- `target/api-blueprint-openapi-converter-1.0.0.jar`
- `target/api-blueprint-openapi-converter-1.0.0-jar-with-dependencies.jar`

## 実行

### 1) 単一ファイル変換（従来どおり）

```bash
java -jar target/api-blueprint-openapi-converter-1.0.0-jar-with-dependencies.jar \
  src/test/resources/wazahyo.apib \
  target/openapi.yaml
```

### 2) 設定ファイルで実行モードを制御

ルートの `converter.properties` で実行モードと入出力先を管理します。

```properties
# 実行モード: single または batch
mode=batch

# single モード時
single.input=src/test/resources/wazahyo.apib
single.output=target/openapi.yaml

# batch モード時
batch.inputDir=input
batch.outputDir=output
batch.recursive=false
```

`mode=batch` の場合、`batch.inputDir` 配下の `.apib` をまとめて変換し、
`batch.outputDir` 配下に同じ構造で `.yaml` を出力します。

実行:

```bash
java -jar target/api-blueprint-openapi-converter-1.0.0-jar-with-dependencies.jar
```

設定ファイルを切り替える場合:

```bash
java -jar target/api-blueprint-openapi-converter-1.0.0-jar-with-dependencies.jar \
  --config ./path/to/converter.properties
```

### 3) IDE から実行（ソース指定しやすい設定）

IDE の実行構成で `main class = xyz.livlog.converter.BlueprintConverterApplication` を指定し、
`Program arguments` は不要（または `--config ...` のみ）にします。

- 例: `--config ./converter.properties`
- 入力/出力先や single/batch の切替は `converter.properties` 側で管理

## サンプル

入力サンプル:
- `src/test/resources/wazahyo.apib`

## 補足

このコンバータは「実務で使える骨組みを自動生成する」ことを目的にしています。  
複雑な MSON や API Blueprint 独自機能を完全再現するものではありませんが、今回の和座標 API のような構成は変換できるようにしています。
