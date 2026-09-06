# NexoRPUploader

A [Nexo](https://nexomc.com/products/nexo) resource pack server extension that uploads the built resource pack to **Dropbox** and serves it to players via a direct-download link.


## Requirements

- A server running [Nexo](https://nexomc.com/products/nexo).
- A [Dropbox App](https://www.dropbox.com/developers/apps/create?_tk=pilot_lp&_ad=ctabtn1&_camp=create) with OAuth2 configured, and a long-lived [**refresh token**](https://stackoverflow.com/a/71794390) for that app.


## Configuration

This plugin reads its settings from Nexo's own `plugins/Nexo/settings.yml`, under a `Pack.server` section:

```yaml
Pack:
  server:
    type: DROPBOX
    dropbox:
      app_key: "your-dropbox-app-key"
      app_secret: "your-dropbox-app-secret"
      refresh_token: "your-dropbox-refresh-token"
      filename: "pack.zip"
```

| Key | Description | Default |
|---|---|---|
| `app_key` | Dropbox App key (client ID) | `""` |
| `app_secret` | Dropbox App secret | `""` |
| `refresh_token` | OAuth2 refresh token for your Dropbox app | `""` |
| `filename` | Name of the file uploaded to the app's Dropbox root (e.g. `/pack.zip`) | `pack.zip` |