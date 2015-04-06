#! /bin/bash
if [ -f "gradle.properties" ]; then
        "\n" >> "gradle.properties"
fi

echo -e "signing.keyId=${SIGNING_KEY}" >> "gradle.properties"
echo -e "signing.password=${SIGNING_KEY_PASSWORD}" >> "gradle.properties"
echo -e "signing.secretKeyRingFile=${SIGNING_SECRETKEYRINGFILE}" >> "gradle.properties"
