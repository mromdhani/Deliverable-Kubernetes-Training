# Lab 05- Exposing Services with Ingress
---

Lab Steps:
- [Step 1- Setting Up Traefic Ingress Controller](#step-1--setting-up-traefic-ingress-controller)
- [Step 2 - Path based Routing](#step-2---path-based-routing)
- [Step 3 - Host-based Routing](#step-3---host-based-routing)
- [Step 4 - Mixed Host/Path based Routing](#step-4---mixed-hostpath-based-routing)
- [Step 3- Adding a Traefik Ingress Controller to the BookStore Full Stack App](#step-3--adding-a-traefik-ingress-controller-to-the-bookstore-full-stack-app)
     

# Step 1- Setting Up Traefic Ingress Controller

An ingress is really just a set of rules to pass to a controller that is listening for them. You can deploy a bunch of ingress rules, but nothing will happen unless you have a controller that can process them. A LoadBalancer service could listen for ingress rules, if it is configured to do so.

Ingress sits between the public network (Internet) and the Kubernetes services that publicly expose our Api's implementation. Ingress is capable to provide Load Balancing, SSL termination, and name-based virtual hosting.
Ingress capabilities allows to securely expose multiple API's or Applications from a single domain name.

To set up an ingress, we need to configure a **Ingress Controller** is simply a pod that is configured to interpret ingress rules. The most popular ingress controllers supported by Kubernetes are nginx and [Traefik](https://docs.traefik.io/providers/kubernetes-ingress/), ...

- Install the Traefic Ingress Controller On Minikube
Traefik can be installed in Kubernetes using the Helm chart from <https://github.com/traefik/traefik-helm-chart>. With the command helm version, make sure that you have Helm v3 installed.

  - Add Traefik's chart repository to Helm:
```
helm repo add traefik https://helm.traefik.io/traefik
```
You can update the chart repository by running:
```
helm repo update
```
   - Deploying Traefik
```
helm install traefik traefik/traefik
```

- Exposing the Traefik dashboard
This HelmChart does not expose the Traefik dashboard by default, for security concerns. Thus, there are multiple ways to expose the dashboard. For instance, the dashboard access could be achieved through a port-forward :
```
kubectl port-forward $(kubectl get pods --selector "app.kubernetes.io/name=traefik" --output=name) 9000:9000
```
Access the Dashboard with the url: <http://127.0.0.1:9000/dashboard/>


# Step 2 - Path based Routing

Let's suppose that we have two applications `apple` and `banana`. Each application exposes a ClusterIP Service on the `endpoint /`. We configure Traefik to route the access to these services using the routes `/apple` and `/banana`. This kind of routing is called Path based routing because Traefik will determine the service to route route based on the provide path in the URL.

- Deploy the apple and babana applications. Check that the pods and the services are working.
```
kubectl apply -f unit5-01-app-apple.yaml 
kubectl apply -f unit5-01-app-banana.yaml
```

- Write the Kubernetes Ingress Object in a manifest named `unit5-02-01-ingress-path-based.yaml`. Initialize it as follows.
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: example-ingress-path-based
  annotations:
    kubernetes.io/ingress.class: traefik
    ingress.kubernetes.io/rewrite-target: /
    traefik.ingress.kubernetes.io/router.entrypoints: web
spec:
  rules:
  - http:
      paths:
        - path: /apple
          pathType: Prefix
          backend:
            service:
              name: apple-service
              port:
                number: 5678
        - path: /banana
          pathType: Prefix
          backend:
            service:
              name: banana-service
              port:
                number: 5678
```

- Deploy the ingress using the following command. 
```
kubectl apply -f unit5-02-01-ingress-path-based.yaml
```

- Check that the ingress deployment was successful. Use the following command. View the ingress in Traefik Dashboard UI.
```
 kubectl get ingress
```

- Test the ingress using the following curl commands:
```shell
$ curl  http://<Traefik-External-Loadbalncer-IP>/apple
apple   # This is the expected output
$ curl  http://<Traefik-External-Loadbalncer-IP>/apple
banana  # This is the expected output
```

- Clean up. Remove and the ingress object.
```
$ kubectl delete -f unit5-02-01-ingress-path-based.yaml
```

- Rewrite the ingress using the Traefik `ingressroute` CRD object. Name the manifest `unit5-02-02-ingress-path-based-ingressroute.yaml`. Indication : Use the following YAML content.

```yaml
apiVersion: traefik.containo.us/v1alpha1
kind: IngressRoute
metadata:
  name: example-ingress-path-based-crd
spec:
  entryPoints:
    - web
  routes:
    - match: PathPrefix(`/apple`) 
      kind: Rule
      services:
        - name: apple-service
          port: 5678
          strategy: RoundRobin
          weight: 10
      middlewares:
        - name: do-stripprefix            
    - match: PathPrefix(`/banana`) 
      kind: Rule
      services:
        - name: banana-service
          port: 5678
          strategy: RoundRobin
          weight: 10
      middlewares:
        - name: do-stripprefix
---
apiVersion: traefik.containo.us/v1alpha1
kind: Middleware
metadata:
  name: do-stripprefix
spec:
  stripPrefix:
    prefixes:
      - /apple
      - /banana
```
Notice that we require a middleware object in order to strip the path since the service does not require a path.

- Deploy the `ingressroute`, inpect the route on the Dashoard and check it is working by issuing the `curl` commands to the ingrss.
- Clean up. Remove and the ingressroute objects.
```
$ kubectl delete -f unit5-02-02-ingress-path-based-ingressroute.yaml
```

# Step 3 - Host-based Routing

In this routing mode, the hostname in the provided request is used to route the request to the corresponding service. Let's create two virtual hosts for our couple of services. We will name them `apple.fruits.org` and `banana.fruits.org`.

- Identify the external Loadbalancer IP address of the Trafik Service. Use the following command.
```
kubectl get svc traefik
NAME      TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)                      AGE
traefik   LoadBalancer   10.104.205.175   10.104.205.175   80:32544/TCP,443:32292/TCP   123m 
```

- Navigate to the hosts file of windows which is located in `‪C:\Windows\System32\drivers\etc\hosts`. Open it with NotPad++, and append a  new line for our two sites. Please adapt the IP to your external IP that you have got in the previous step.

```
10.104.205.175 banana.fruits.org apple.fruits.org 
```

- Write the Kubernetes Ingress Object in a manifest named `unit5-02-03-ingress-host-based.yaml`. Initialize it as follows.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: example-ingress-host-based
  annotations:
    kubernetes.io/ingress.class: traefik
    # ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: apple.fruits.org
    http:
      paths:
        - path: /
          pathType: Prefix
          backend:
            service:
              name: apple-service
              port:
                number: 5678
  - host: banana.fruits.org
    http:
      paths:              
        - path: /
          pathType: Prefix
          backend:
            service:
              name: banana-service
              port:
                number: 5678
```

- Deploy the ingress using the following command. 
```
kubectl apply -f unit5-02-03-ingress-host-based.yaml
```

- Check that the ingress deployment was successful. Use the following command. View the ingress in Traefik Dashboard UI.
```
 kubectl get ingress
```

- Test the ingress using the following curl commands:
```shell
$ curl  http://apple.fruits.org
apple   # This is the expected output
$ curl  http://banana.fruits.org
banana  # This is the expected output
```

- Clean up. Remove and the ingress object.
```
$ kubectl delete -f unit5-02-03-ingress-host-based.yaml
```

- Rewrite the ingress using the Traefik `ingressroute` CRD object. Name the manifest `unit5-02-04-ingress-host-based-ingressroute.yaml`. Indication : Use the following YAML content.

```yaml
apiVersion: traefik.containo.us/v1alpha1
kind: IngressRoute
metadata:
  name: example-ingress-host-based-crd
spec:
  entryPoints:
    - web
  routes:
    - match:  Host(`apple.fruits.org`) 
      kind: Rule
      services:
        - name: apple-service
          port: 5678
          strategy: RoundRobin
          weight: 10            
    - match: Host(`banana.fruits.org`)
      kind: Rule
      services:
        - name: banana-service
          port: 5678
          strategy: RoundRobin
          weight: 10
# Middleware are not required here because we are accessing the 
# services at their native endpoint which is /.
```

- Deploy the `ingressroute`, inpect the route on the Dashoard and check it is working by issuing the `curl` commands to the ingress.
- Clean up. Remove and the ingressroute object.
```
$ kubectl delete -f unit5-02-03-ingress-host-based.yaml.yaml
```

# Step 4 - Mixed Host/Path based Routing

Let's now experiment the mixed Host and Path based routing.

- Write the Kubernetes Ingress Object in a manifest named `unit5-02-05-ingress-host-path-based.yaml`. Initialize it as follows.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: example-ingress-host-based
  annotations:
    kubernetes.io/ingress.class: traefik
    ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: apple.fruits.org
    http:
      paths:
        - path: /apple
          pathType: Prefix
          backend:
            service:
              name: apple-service
              port:
                number: 5678
  - host: banana.fruits.org
    http:
      paths:              
        - path: /banana
          pathType: Prefix
          backend:
            service:
              name: banana-service
              port:
                number: 5678
```

- Deploy the ingress using the following command. 
```
kubectl apply -f unit5-02-05-ingress-host-path-based.yaml
```

- Check that the ingress deployment was successful. Use the following command. View the ingress in Traefik Dashboard UI.
```
 kubectl get ingress
```

- Test the ingress using the following curl commands:
```shell
$ curl  http://apple.fruits.org/apple
apple   # This is the expected output
$ curl  http://banana.fruits.org/banana
banana  # This is the expected output
```

- Clean up. Remove and the ingress object.
```
$ kubectl delete -f unit5-02-05-ingress-host-path-based.yaml
```

- Rewrite the ingress using the Traefik `ingressroute` CRD object. Name the manifest `unit5-02-06-ingress-host-path-based-ingressroute.yaml`. Indication : Use the following YAML content.

```yaml
apiVersion: traefik.containo.us/v1alpha1
kind: IngressRoute
metadata:
  name: example-ingress-host-based-crd
spec:
  entryPoints:
    - web
  routes:
    - match:  Host(`apple.fruits.org`) && PathPrefix(`/apple`) 
      kind: Rule
      services:
        - name: apple-service
          port: 5678
          strategy: RoundRobin
          weight: 10   
      middlewares:
        - name: do-stripprefix         
    - match: Host(`banana.fruits.org`) && PathPrefix(`/banana`) 
      kind: Rule
      services:
        - name: banana-service
          port: 5678
          strategy: RoundRobin
          weight: 10
      middlewares:
        - name: do-stripprefix
---
apiVersion: traefik.containo.us/v1alpha1
kind: Middleware
metadata:
  name: do-stripprefix
spec:
  stripPrefix:
    prefixes:
      - /apple
      - /banana
```
Notice that we require a middleware object in order to strip the path since the service does not require a path.

- Clean up. Remove and the ingressroute object.
```
$ kubectl delete -f unit5-02-06-ingress-host-path-based-ingressroute.yaml
```

# Step 3- Adding a Traefik Ingress Controller to the BookStore Full Stack App

- Refactor the access Layer to services using an Ingress Object
- Use the following specification for your ingress
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: bookstore-ingress
  annotations:
    kubernetes.io/ingress.class: traefik
spec:
  rules:
  - host: bookstore.minikube
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
             name: frontend
             port:
               number: 80
      - path: /books
        pathType: Prefix
        backend:
          service:
             name: backend-api
             port:
               number: 80
      - path: /categories
        pathType: Prefix
        backend:
          service:
             name: backend-api
             port:
               number: 80
```
- Check that the new version of the application works.  
- Discuss the advantages of the Ingress in the case of BookStore app.