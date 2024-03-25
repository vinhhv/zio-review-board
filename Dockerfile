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

### NGINX
# RUN ufw allow 'Nginx Full'
# RUN ufw allow 'OpenSSH'

RUN mkdir -p /var/www/staging.swishprograms.com/html
COPY staging.swishprograms.com /etc/nginx/sites-available/staging.swishprograms.com
RUN ln -s /etc/nginx/sites-available/staging.swishprograms.com /etc/nginx/sites-enabled
RUN sed -i '/# server_names_hash_bucket_size/c\server_names_hash_bucket_size 64;' /etc/nginx/nginx.conf

#### Scala
ENV SBT_VERSION=1.9.7
ENV SCALA_VERSION=3.3.1

# Install sbt
RUN curl -fL https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz | tar xz -C /usr/local && \
  ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

# Install Scala
RUN curl -L "https://github.com/scala/scala3/releases/download/${SCALA_VERSION}/scala3-${SCALA_VERSION}.tar.gz" | tar xz -C /usr/local && \
  ln -s /usr/local/scala-${SCALA_VERSION}/bin/scala /usr/bin/scala

# Create non-root user
RUN groupadd -r developer && useradd -r -g developer -G sudo -m -s /bin/bash developer

# Switch to the non-root user
USER developer
ENV HOME /home/developer
# Set the working directory
WORKDIR /home/developer

WORKDIR $HOME

### APP
COPY modules/app/dist-staging/* /var/www/staging.swishprograms.com/html

COPY swishprograms-staging_3.jar $HOME/server.jar

RUN java --version

COPY start.sh $HOME/start.sh

CMD ["/home/developer/start.sh"]
