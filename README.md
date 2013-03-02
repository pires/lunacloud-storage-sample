1) You must have Javaa 6 and Maven 3.0.3+ installed
2) You must have Lunacloud Java SDK available on your Maven repo
2.1) Clone https://github.com/pires/lunacloud-sdk-java
2.2) mvn clean install -Dgpg.skip=true
3) mvn clean package exec:java
