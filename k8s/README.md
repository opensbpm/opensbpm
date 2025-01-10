
'''
kubectl --kubeconfig ~/.kube/opensbpm@hetzner.yaml -k k8s apply
'''

== only needed for local development
'''
kubectl --kubeconfig ~/.kube/opensbpm@hetzner.yaml create -n opensbpm secret generic engine-db-password --from-literal=password=OpenSBPM
kubectl --kubeconfig ~/.kube/opensbpm@hetzner.yaml create -n opensbpm secret generic keycloak-db-password --from-literal=password=OpenSBPM
'''

=== fetch tls secret
'''
kubectl --kubeconfig ~/.kube/opensbpm@hetzner.yaml get secret -n opensbpm opensbpm-tls -o yaml > k8s/secret-tls.yaml
'''

=== ingress logs
'''
kubectl --kubeconfig ~/.kube/opensbpm@hetzner.yaml logs -n ingress -l name=nginx-ingress-microk8s
'''

== Dashboard access with port-forwarding

(see https://microk8s.io/docs/addon-dashboard )
'''
microk8s kubectl port-forward -n kube-system service/kubernetes-dashboard --address 0.0.0.0 10443:443
'''

Token
'''
microk8s kubectl create token default
'''


==  Prometheus UI with port-forwarding
(see https://www.server-world.info/en/note?os=Ubuntu_24.04&p=microk8s&f=8 )

'''
root@dlp:~# microk8s kubectl port-forward -n observability service/prometheus-operated --address 0.0.0.0 9090:9090
'''
Use http://168.119.255.223:9090/


== Grafana access with port-forwarding
(see https://www.server-world.info/en/note?os=Ubuntu_24.04&p=microk8s&f=8 )
'''
microk8s kubectl port-forward -n observability service/kube-prom-stack-grafana --address 0.0.0.0 3000:80 
'''
Use http://168.119.255.223:3000/

Micrometer Dashboard
https://grafana.com/grafana/dashboards/4701-jvm-micrometer/
