# Save git information
git_name=$(git config user.name)
git_email=$(git config user.email)

# Setup Java 11
if [ -f /usr/lib/jvm/temurin-11-jdk-amd64/bin/java ]; then
    echo "Java 11 already installed"
else
    sudo apt install -y wget apt-transport-https
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /usr/share/keyrings/adoptium.asc
    echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
    sudo apt update
    sudo apt install -y temurin-11-jdk
fi

# Setup BuildTools
cd ..
if [ ! -d BuildTools ]; then
    mkdir BuildTools
fi
cd BuildTools
if [ -f BuildTools.jar ]; then
    echo "BuildTools.jar already exists, skipping download"
else
    echo "Downloading BuildTools.jar"
    wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
fi

# Build 1.16.5
if [ -f ~/.m2/repository/org/bukkit/craftbukkit/1.16.5-R0.1-SNAPSHOT/craftbukkit-1.16.5-R0.1-SNAPSHOT.jar ]; then
    echo "1.16.5 already exists, skipping build"
else
    /usr/lib/jvm/temurin-11-jdk-amd64/bin/java -jar BuildTools.jar --rev 1.16.5 --compile craftbukkit
fi

# Build 1.18.2
if [ -f ~/.m2/repository/org/spigotmc/spigot/1.18.2-R0.1-SNAPSHOT/spigot-1.18.2-R0.1-SNAPSHOT.jar ]; then
    echo "1.18.2 already exists, skipping build"
else
    java -jar BuildTools.jar --rev 1.18.2 --remapped
fi

# Build 1.19.4
if [ -f ~/.m2/repository/org/spigotmc/spigot/1.19.4-R0.1-SNAPSHOT/spigot-1.19.4-R0.1-SNAPSHOT.jar ]; then
    echo "1.19.4 already exists, skipping build"
else
    java -jar BuildTools.jar --rev 1.19.4 --remapped
fi

# Build 1.20.4
if [ -f ~/.m2/repository/org/spigotmc/spigot/1.20.4-R0.1-SNAPSHOT/spigot-1.20.4-R0.1-SNAPSHOT.jar ]; then
    echo "1.20.4 already exists, skipping build"
else
    java -jar BuildTools.jar --rev 1.20.4 --remapped
fi

# Restore git information
git config --global user.name "$git_name"
git config --global user.email "$git_email"
