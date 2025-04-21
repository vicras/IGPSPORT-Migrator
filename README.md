# How to get Strava oAuth code:
```
  GET
  https://www.strava.com/oauth/authorize?
  client_id=YOUR_CLIENT_ID
  &response_type=code
  &redirect_uri=http://localhost/exchange_token
  &approval_prompt=force
  &scope=activity:write
```

# How to get Strava oAuth token:
```
  POST
  https://www.strava.com/oauth/token
  PAYLOAD
  {
    "client_id": "YOUR_CLIENT_ID",
    "client_secret": "YOUR_CLIENT_SECRET",
    "code": "YOUR_CLIENT_CODE",
    "grant_type": "authorization_code"
  }
```
