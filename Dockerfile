FROM eclipse-temurin:21-jdk-jammy

ENV ANDROID_SDK_URL https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
ENV ANDROID_API_LEVEL android-37.0
ENV ANDROID_BUILD_TOOLS_VERSION 37.0.0
ENV ANDROID_HOME /usr/local/android-sdk-linux
ENV ANDROID_VERSION 37.0
ENV PATH $PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/bin

RUN apt-get update && \
    apt-get install --no-install-recommends -y curl unzip ca-certificates && \
    mkdir "$ANDROID_HOME" .android && \
    cd "$ANDROID_HOME" && \
    curl -o sdk.zip $ANDROID_SDK_URL && \
    unzip sdk.zip && \
    rm sdk.zip && \
# Download Android SDK
yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME && \
sdkmanager --update --sdk_root=$ANDROID_HOME && \
sdkmanager --sdk_root=$ANDROID_HOME "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools"
# Install Fastlane \
COPY Gemfile Gemfile.lock ./
RUN apt-get update && \
apt-get install --no-install-recommends -y --allow-unauthenticated build-essential git ruby-full graphicsmagick && \
gem install bundler && \
bundle install && \
gem install fastlane-plugin-firebase_app_distribution fastlane-plugin-badge fastlane-plugin-increment_version_code && \
# Clean up
rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
apt-get autoremove -y && \
apt-get clean