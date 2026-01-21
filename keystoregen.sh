#!/bin/sh
# Generate a keystore file with a given password
keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass $1
keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg HMacSHA384 -keysize 2048 -alias HS384 -keypass $1
keytool -genseckey -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg HMacSHA512 -keysize 2048 -alias HS512 -keypass $1
keytool -genkey -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg RSA -keysize 2048 -alias RS256 -keypass $1 -sigalg SHA256withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkey -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg RSA -keysize 2048 -alias RS384 -keypass $1 -sigalg SHA384withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkey -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg RSA -keysize 2048 -alias RS512 -keypass $1 -sigalg SHA512withRSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg EC -keysize 256 -alias ES256 -keypass $1 -sigalg SHA256withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg EC -keysize 256 -alias ES384 -keypass $1 -sigalg SHA384withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
keytool -genkeypair -keystore keystore.jceks -storetype jceks -storepass $1 -keyalg EC -keysize 256 -alias ES512 -keypass $1 -sigalg SHA512withECDSA -dname "CN=,OU=,O=,L=,ST=,C=" -validity 360
# Import keystore jceks to pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12
keytool -importkeystore -srckeystore keystore.jceks -destkeystore keystore.jceks -deststoretype pkcs12