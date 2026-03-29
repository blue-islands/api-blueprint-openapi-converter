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

```bash
java -jar target/api-blueprint-openapi-converter-1.0.0-jar-with-dependencies.jar \
  src/test/resources/wazahyo.apib \
  target/openapi.yaml
```

## サンプル

入力サンプル:
- `src/test/resources/wazahyo.apib`

## 補足

このコンバータは「実務で使える骨組みを自動生成する」ことを目的にしています。  
複雑な MSON や API Blueprint 独自機能を完全再現するものではありませんが、今回の和座標 API のような構成は変換できるようにしています。
