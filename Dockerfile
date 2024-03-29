FROM eclipse-temurin:21

### JAVA
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"

RUN rm /bin/sh && ln -s /bin/bash /bin/sh

RUN apt-get update \
  && apt-get install -y \
  curl \
  git \
  golang \
  sudo \
  vim \
  wget \
  unzip \
  zip \
  nginx \
  npm \
  nodejs \
  ca-certificates \
  gnupg \
  && rm -rf /var/lib/apt/lists/*

### Stripe
RUN curl -s https://packages.stripe.dev/api/security/keypair/stripe-cli-gpg/public | gpg --dearmor | sudo tee /usr/share/keyrings/stripe.gpg
RUN echo "deb [signed-by=/usr/share/keyrings/stripe.gpg] https://packages.stripe.dev/stripe-cli-debian-local stable main" | sudo tee -a /etc/apt/sources.list.d/stripe.list
RUN sudo apt update
RUN sudo apt install -y stripe

# Create non-root user
RUN groupadd -r developer && useradd -r -g developer -G sudo -m -s /bin/bash developer

### NGINX
# RUN ufw allow 'Nginx Full'
# RUN ufw allow 'OpenSSH'

# Set up directories NGINX needs to write to
RUN mkdir -p /var/log/nginx /var/lib/nginx /var/tmp/nginx /var/www/swishprograms.com/html && \
  chown -R developer:developer /var/log/nginx /var/lib/nginx /var/tmp/nginx /var/www/swishprograms.com/html && \
  chmod -R 775 /var/log/nginx /var/lib/nginx /var/tmp/nginx /var/www/swishprograms.com/html

COPY swishprograms.com /etc/nginx/sites-available/swishprograms.com
RUN ln -s /etc/nginx/sites-available/swishprograms.com /etc/nginx/sites-enabled
RUN sed -i '/# server_names_hash_bucket_size/c\server_names_hash_bucket_size 64;' /etc/nginx/nginx.conf
RUN sed -i 's|pid /run/nginx.pid;|pid /tmp/nginx.pid;|' /etc/nginx/nginx.conf

# Need to remove anything listening on port 80, or fly.io will deny permission
RUN rm /etc/nginx/sites-enabled/default

#### Scala
ENV SBT_VERSION=1.9.7
ENV SCALA_VERSION=3.3.1

# Install sbt
RUN curl -fL https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz | tar xz -C /usr/local && \
  ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

# Install Scala
RUN curl -L "https://github.com/scala/scala3/releases/download/${SCALA_VERSION}/scala3-${SCALA_VERSION}.tar.gz" | tar xz -C /usr/local && \
  ln -s /usr/local/scala-${SCALA_VERSION}/bin/scala /usr/bin/scala

# Switch to the non-root user
USER developer
ENV HOME /home/developer
# Set the working directory
WORKDIR /home/developer

WORKDIR $HOME

### APP

# If fly.io does not pick up any changes, try deleting the trailing '/'
COPY modules/app/dist-prod/* /var/www/swishprograms.com/html/

COPY swishprograms.jar $HOME/server.jar

RUN java --version

COPY start.sh $HOME/start.sh

CMD ["/home/developer/start.sh"]
