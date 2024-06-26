# Swish Programs

### Compiling backend server

Run `sbt server/assembly`
Then copy .jar file to local directory: `cp [LOCATION] swishprograms.jar`

### Compiling frontend

Run `sbt -J-Xms2G -J-Xmx2G "project app" fullOptJS"`
Run `cd modules/app && npm run build-prod`

### Set fly.io secrets (do not add to git)

```
fly secrets set \
 DATABASE_JDBC_URL=jdbc:postgresql://swishprograms-db.flycast:5432/swishprograms?sslmode=disable \
 DATABASE_USER=postgres \
 DATABASE_PASS=[FILL_IN] \
 BACKEND_HTTP_PORT=4041 \
 JWT_SECRET=[FILL_IN] \
 JWT_TTL=864000 \
 STRIPE_KEY=[FILL_IN] \
 STRIPE_PRICE=[FILL_IN] \
 STRIPE_SUCCESS_URL=http://localhost:1234/profile \
 STRIPE_CANCEL_URL=http://localhost:1234/ \
 STRIPE_SECRET=[FILL_IN] \
 EMAIL_HOST=smtp.ethereal.email \
 EMAIL_PORT=[FILL_IN] \
 EMAIL_USER=[FILL_IN] \
 EMAIL_PASS=[FILL_IN] \
 EMAIL_BASE_URL=http://localhost:1234 \
 OPENAI_SECRET_KEY=[FILL_IN] \
 SENDGRID_API_KEY=[FILL_IN] \
 SENDGRID_BASE_URL=https://swishprograms.com \
 SENDGRID_FROM_ADDRESS=info@swishprograms.com
```

### Deploy

`fly deploy`

_NOTE_: The deployment might not recognize changes to modules/dist-prod/ and could ignore the new files
