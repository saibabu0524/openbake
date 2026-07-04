# Sri Vinayaka Bakery Home — Coming Soon

A Spring Boot app packaged as a WAR that serves the "Coming Soon" landing page for [srivinayakabakeryhome.com](https://srivinayakabakeryhome.com) while the main site is under construction. It deploys to an external Tomcat and can also run standalone for local development.

## Features

- Professional dark-theme landing page built around the brand logo (`vinayaka_logo.png`)
- Live countdown to the launch date
- "Notify Me" email capture — emails are saved to a CSV file (no database required)
- Duplicate-email detection and server-side email validation
- Context-path agnostic: works deployed as `ROOT.war` or under `/coming-soon`
- Fully offline: fonts are self-hosted and icons are emoji — no CDN or internet needed to serve the page

## Requirements

- Java 17+
- Maven 3.8+
- **External Tomcat 10.1+** (Spring Boot 3 uses Jakarta EE; Tomcat 9 will NOT work)

## Deploy to external Tomcat

```bash
cd comming_soon
mvn clean package
```

This produces `target/coming-soon.war`. Then either:

- **Serve at the domain root** (recommended for srivinayakabakeryhome.com): copy the WAR to Tomcat as `ROOT.war`

  ```bash
  cp target/coming-soon.war $CATALINA_HOME/webapps/ROOT.war
  ```

  The site is then available at `http://<host>:8080/`

- **Serve under a context path**: copy it as-is

  ```bash
  cp target/coming-soon.war $CATALINA_HOME/webapps/
  ```

  The site is then available at `http://<host>:8080/coming-soon/`

Tomcat auto-deploys the WAR on startup (or hot-deploys if already running).

## Run locally (no Tomcat needed)

```bash
cd comming_soon
mvn spring-boot:run
```

Then open http://localhost:8085

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8085` | HTTP port — **local runs only**; ignored under external Tomcat (Tomcat's own port applies) |
| `app.subscribers-file` | `${user.home}/vinayaka-subscribers.csv` | Where notify-me emails are appended |

Override at deploy time via environment variables, e.g. `APP_SUBSCRIBERS_FILE=/var/data/subscribers.csv` in Tomcat's `setenv.sh`.

The countdown launch date is set in `src/main/resources/static/js/main.js` (`LAUNCH_DATE` constant).

## API

`POST api/subscribe` (relative to the app's context path) with body `{"email": "someone@example.com"}`

- `200` — subscribed, email appended to the CSV
- `400` — invalid email
- `409` — already subscribed
