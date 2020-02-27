
## 替换：
替换YourSecretPhrase为你账户的助计词，把YourPoolAccountPublicKey替换成你的节点的账户公钥

## 执行：
curl -g --location --request POST "http://127.0.0.1:8025/vol?requestType=setGlobalParameter&secretPhrase=YourSecretPhrase&deadline=300&feeNQT=200000000&poolerAddressList={%22addPool%22:[{%22account%22:%22YourPoolAccountPublicKey%22}]}&broadcast=true&poolCount=1"



