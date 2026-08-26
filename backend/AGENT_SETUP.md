# Library Agent setup

The application reads secrets from operating-system environment variables. It does not load or commit a local secret file.

## PowerShell

Create a fresh JWT signing secret and set the rotated DeepSeek key in the same terminal that starts Spring Boot:

```powershell
$jwtBytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:DEEPSEEK_API_KEY = "your-new-rotated-key"
mvn spring-boot:run
```

For persistent local development, store these variables in the Windows user environment or a dedicated secret manager. Do not put real values in `application.yml`, `.env.example`, source files, shell history, or screenshots.

## Agent boundaries

The model can dynamically call a bounded registry of tools for:

- catalog search, book details, recommendations and library statistics;
- the authenticated user's borrowing, wish list, reservation and notification data;
- reader action drafts for wish list, reservation and renewal operations;
- staff-only catalog management proposals for up to five supplied titles.

The model never writes catalog or reader data directly. Every write first becomes a five-minute draft and requires an explicit confirmation. Catalog proposals return real candidate records and role-filtered choices: librarians may add books, change stock, disable borrowing and restore borrowing; only administrators may hard-delete a book. Confirmation rechecks the current JWT user, current role, stock limits and business references before executing.

The server validates all model-generated arguments, limits each run to a bounded number of iterations and tool calls, binds personal tools to the JWT principal, and records a minimal execution audit without storing secrets.
