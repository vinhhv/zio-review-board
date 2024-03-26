server {
        listen 8080;
        listen [::]:8080;

        root /var/www/swishprograms.com/html;
        index index.html index.htm index.nginx-debian.html;

        server_name swishprograms.com www.swishprograms.com;

        location /api {
                proxy_pass http://localhost:4041;
        }
        
        error_page 404 =200 /index.html;
}