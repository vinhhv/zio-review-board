server {
        listen 80;
        listen [::]:80;

        root /var/www/staging.swishprograms.com/html;
        index index.html index.htm index.nginx-debian.html;

        server_name staging.swishprograms.com www.staging.swishprograms.com;

        location /api {
                proxy_pass http://localhost:4041;
        }
        
        error_page 404 =200 /index.html
}