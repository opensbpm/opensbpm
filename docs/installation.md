# Installation for local development

Create a virtual machine with lastest Ubuntu LTS Server https://ubuntu.com/download/server
Choose microk8s

#Configure cluster

Edit #/var/snap/microk8s/current/certs/csr.conf.template# and add

After ~DNS.5~
```
DNS.99 = opensbpm.local
IP.100 = 192.168.66.117
```

Dump kubectl config adn save it locally in ~/.kube 
`microk8s config`
Update names to your needs.


`kubectl apply -k .`

## Install microk8s plugins

```
microk8s enable dashboard
microk8s enable dns
microk8s enable ingress
```

Dashboard is available at https://\<ip\>:10443

Create token to access dashboard
```
microk8s kubectl create token default
```

## micork8s zurücksetzen
`microk8s reset --destroy-storage`
