# Lab 04- Deployment and Scalability
---
- [Step 1 - Rolling out and Rolling back Deployments](#step-1---rolling-out-and-rolling-back-deployments)
- [Step 2 - Implementing Recreate and Rolling Update release Strategies](#step-2---implementing-recreate-and-rolling-update-release-strategies)
- [Step 3 - Implementing Canary Release Strategy](#step-3---implementing-canary-release-strategy)
  - [Setting up Canary Deployment on Kubernetes](#setting-up-canary-deployment-on-kubernetes)
  - [Testing the Canary deployment](#testing-the-canary-deployment)
- [Step 4 - Horizontal Pod Autoscaler Walkthrough](#step-4---horizontal-pod-autoscaler-walkthrough)
# Step 1 - Rolling out and Rolling back Deployments

- Rollout a Deployment

 Let's recreate the application `nginx-app-deploy` described in previous section and having the `unit4-01-deployment.yaml` file.
  ```
    kubectl create -f unit4-01-deployment.yaml --record
  ```  
 Let's practice a rollout by setting the replicas to `10` and setting the `nginx` image to the `nginx:1.18` version.  
    ```
      replicas: 10
      ...
      image: nginx:1.18
    ```
  - Apply the rollout of the application using the following command:
   ```
   kubectl apply -f unit4-01-deployment.yaml --record
   ```
  - You can view the rollout status using the command:
    ```shell
    kubectl rollout status deployment/nginx-app-deploy
    deployment "nginx-app-deploy" successfully rolled out
    ```
  - You can view the rollout history using the command:
    ```shell
    kubectl rollout history deployment/nginx-app-deploy
    REVISION  CHANGE-CAUSE
    1         kubectl.exe create --filename=.\unit4-01-deployment.yaml --record=true
    2         kubectl.exe apply --filename=.\unit4-01-deployment.yaml --record=true
    ```
  - To inspect the details of a given revision, run:
  ```shell
  kubectl rollout history deployment/nginx-app-deploy --revision=2
  ```
- Rolling Back a Deployment

   Sometimes, you may want to rollback a Deployment; for example, when the Deployment is not stable, such as crash looping. By default, all of the Deployment's rollout history is kept in the system so that you can rollback anytime you want (you can change that by modifying revision history limit).

  - Suppose that you made a typo while updating the Deployment, by putting the image name as `nginx:1.77` instead of `nginx:1.7`.

   ```shell
  kubectl set image deployment/nginx-app-deploy nginx-container=nginx:1.77 --record=true
  ```
  - The rollout gets stuck. You can verify it by checking the rollout status:
  ```shell
  kubectl rollout status deployment/nginx-app-deploy
  ```
The output is similar to this:
```shell
Waiting for deployment "nginx-app-deploy" rollout to finish: 5 out of 10 new replicas have been updated...
```
Press Ctrl-C to stop the above rollout status watch.
  - Look at the ReplicaSets. There are only ready 8 replicas in the previous replicaset, while no replicas are ready from the new replicaset. 
```shell
kubectl get rs
NAME                          DESIRED   CURRENT   READY   AGE
nginx-app-deploy-6dcc4bf49d   0         0         0       7m32s
nginx-app-deploy-77b4db474f   5         5         0       55s
nginx-app-deploy-868755f86b   8         8         8       7m9s
```
- Looking at the Pods created, you see that 5 Pods created by new ReplicaSet is stuck in an image pull loop.
```shell
kubectl get pods
```
The output is similar to this:
```shell
NAME                                READY   STATUS             RESTARTS   AGE
nginx-app-deploy-77b4db474f-4vsws   0/1     ImagePullBackOff   0          2m53s
nginx-app-deploy-77b4db474f-5xn8b   0/1     ImagePullBackOff   0          2m53s
nginx-app-deploy-77b4db474f-fpd62   0/1     ImagePullBackOff   0          2m53s
nginx-app-deploy-77b4db474f-g4j9l   0/1     ImagePullBackOff   0          2m53s
nginx-app-deploy-77b4db474f-tggtv   0/1     ImagePullBackOff   0          2m53s
nginx-app-deploy-868755f86b-727xh   1/1     Running            0          9m7s
nginx-app-deploy-868755f86b-9tz6l   1/1     Running            0          9m7s
nginx-app-deploy-868755f86b-dmf9z   1/1     Running            0          9m7s
nginx-app-deploy-868755f86b-k6rwx   1/1     Running            0          8m59s
nginx-app-deploy-868755f86b-k9rhp   1/1     Running            0          9m7s
nginx-app-deploy-868755f86b-ltbc5   1/1     Running            0          9m7s
nginx-app-deploy-868755f86b-n5vgp   1/1     Running            0          8m40s
nginx-app-deploy-868755f86b-vn6wl   1/1     Running            0          8m51s
```

  > **Note:** The Deployment controller stops the bad rollout automatically, and stops scaling up the new ReplicaSet. This depends on the rollingUpdate parameters (**maxUnavailable** specifically) that you have specified. Kubernetes by default sets the value to `25%`.
- Rolling Back to a Previous Revision:
Now you've decided to undo the current rollout and rollback to the previous revision.
```shell
kubectl rollout undo deployment/nginx-app-deploy
```
The output is similar to this:
```shell
deployment.apps/nginx-app-deploy rolled back
```
Alternatively, you can rollback to a specific revision by specifying it with `--to-revision`:
```shell
kubectl rollout undo deployment/nginx-app-deploy --to-revision=2
```
The Deployment is now rolled back to a previous stable revision. 
 - Check if the rollback was successful and the Deployment is running as expected, run:
  ```shell
  kubectl get deployment nginx-app-deploy
  ```
 - Check if the 10 pods are running, run:
  ```shell
  kubectl get pods
  ```
- Clean Up 

 Delete the Deployment using the `kubectl delete deploy nginx-app-deploy ` command. Check that this will remove the ReplicaSet and the Pods. To do the check,  run `kubectl get rs` and `kubectl get pods`. 

# Step 2 - Implementing Recreate and Rolling Update release Strategies
Kubernetes provides two ways to release an application, it is necessary to choose the right strategy to make your infrastructure reliable during an application update.
1. **Recreate**. In this type of very simple deployment, all of the old pods are killed all at once and get replaced all at once with the new ones.
You may choose the deployment strategies depending on your goal. For example, you may need to roll out changes to a specific environment for more testing, or you may want to do some user testing before making a feature 'Generally Available'. 
2. **Rolling Update**. The rolling deployment is the standard default deployment to Kubernetes. It works by slowly, one by one, replacing pods of the previous version of your application with pods of the new version without any cluster downtime.

-  Experimenting the Recreate Rollout Strategy
  - Create a deployment manifest named `unit4-02-rollout-recreate.yaml` the strategy is of type **Recreate**. This is the content of that manifest.
    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: redis-app-recreate
      labels:
        app: redis-app-recreate
    spec:
      replicas: 10
      strategy:
            type: Recreate      
      selector:
        matchLabels:
          app: redis-app-recreate
      template:
        metadata:
          labels:
            app: redis-app-recreate
        spec:
          containers:
            - name: redis
              image: redis:latest
      ```
  - Let's apply the deployment.
    ```    
     kubectl apply -f unit4-02-rollout-recreate.yaml --record=true
    ```  
    Type the command `kubectl get pods` and you will see that there are 10 running pods.
  - Let's try to do a rollout with an erronous image tag. the tag `55` doesn't exist.
    ```    
     kubectl set image deployment/redis-app-recreate redis=redis:55 --record=true
    ```  
    - The rollout fails. Let's view its status Let's try to do a rollout with an erronous image tag. the tag `55` doesn't exist.
    ```    
      kubectl rollout status deployment/redis-app-recreate
    ```
    It displays     
    ```shell
    Waiting for deployment "redis-app-recreate" rollout to finish: 0 out of 10 new replicas have been updated...
    ```.
    Enter `CTL-C`to quit the console.
  - View the state of the deployment
    ```
     kubectl get deploy
    NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
    redis-app-recreate   0/10    10           0           68s
    ```
  - List the pods : You notice that none of the pods is available.
    ```shell
      kubectl get pods
      NAME                                 READY   STATUS             RESTARTS   AGE
      redis-app-recreate-7f6559cf8-5hcnw   0/1     ImagePullBackOff   0          3m21s
      redis-app-recreate-7f6559cf8-6vmkk   0/1     ErrImagePull       0          3m21s
      redis-app-recreate-7f6559cf8-cd79p   0/1     ImagePullBackOff   0          3m21s
      redis-app-recreate-7f6559cf8-dz7tp   0/1     ErrImagePull       0          3m21s
      redis-app-recreate-7f6559cf8-frjfl   0/1     ImagePullBackOff   0          3m21s
      redis-app-recreate-7f6559cf8-h8xns   0/1     ImagePullBackOff   0          3m21s
      redis-app-recreate-7f6559cf8-jdds4   0/1     ErrImagePull       0          3m21s
      redis-app-recreate-7f6559cf8-pfqm2   0/1     ImagePullBackOff   0          3m21s
      redis-app-recreate-7f6559cf8-r4x79   0/1     ImagePullBackOff   0          3m21s
      redis-app-recreate-7f6559cf8-s8qld   0/1     ImagePullBackOff   0          3m21s
   ```
   - Clean Up. Delete the deployment 
    ```    
      kubectl delete deployment/redis-app-recreate
    ```
-  Experimenting the RollingUpdate Rollout Strategy
  - Create a deployment manifest named `unit4-03-rollout-rollingupdate. yaml` the strategy is of type **RollingUpdate**. This is the content of that manifest.
    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: redis-app-rollingupdate
      labels:
        app: redis-app-rollingupdate
    spec:
      replicas: 10
      strategy:
            type: RollingUpdate
            rollingUpdate:
              maxSurge: 25%
              maxUnavailable: 25%      
      selector:
        matchLabels:
          app: redis-app-rollingupdate
      template:
        metadata:
          labels:
            app: redis-app-rollingupdate
        spec:
          containers:
            - name: redis
              image: redis:latest
      ```
    **maxUnavailable** is an optional field that specifies the maximum number of Pods that can be unavailable during the update process.The value can be an absolute number (for example, `5`) or a percentage of desired Pods (for example, `10%`). The default value is `25%`.

    **maxSurge** is an optional field that specifies the maximum number of Pods that can be created over the desired number of Pods. The value can be an absolute number (for example, `5`) or a percentage of desired Pods (for example, `10%`). The default value is `25%`.

  - Let's apply the deployment.
    ```    
     kubectl apply -f unit4-03-rollout-rollingupdate.yaml --record=true
    ``` 
    Type the command `kubectl get pods` and you will see that there are 10 running pods.

  - Let's try to do a rollout with an erronous image tag. the tag `55` doesn't exist.
    ```    
     kubectl set image deployment/redis-app-rollingupdate redis=redis:55 --record=true
    ```  
    - The rollout fails. Let's view its status Let's try to do a rollout with an erronous image tag. the tag `55` doesn't exist.
    ```    
      kubectl rollout status deployment/redis-app-rollingupdate
    ```
    It displays     
    ```shell
    Waiting for deployment "redis-app-rollingupdate" rollout to finish: 5 out of 10 new replicas have been updated...
    ```.
    Enter `CTL-C`to quit the console.
  - View the state of the deployment
    ```
     kubectl get deploy
    NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
    redis-app-rollingupdate   8/10    5            8           2m29s
    ```
  - List the pods : You notice that 8 pods are Running. 5 pods are failing when downloading the image.
    ```shell
      kubectl get pods
      NAME                                       READY   STATUS             RESTARTS   AGE
      redis-app-rollingupdate-5dfcbd7dc8-2hkcn   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-4tvc4   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-59mcv   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-cmkkl   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-dchzh   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-fb7vz   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-q6vww   1/1     Running            0          4m18s
      redis-app-rollingupdate-5dfcbd7dc8-rjgc4   1/1     Running            0          4m18s
      redis-app-rollingupdate-bf5568d75-dvt62    0/1     ImagePullBackOff   0          3m55s
      redis-app-rollingupdate-bf5568d75-gjtph    0/1     ImagePullBackOff   0          3m55s
      redis-app-rollingupdate-bf5568d75-jdqgs    0/1     ImagePullBackOff   0          3m55s
      redis-app-rollingupdate-bf5568d75-qmn7b    0/1     ImagePullBackOff   0          3m55s
      redis-app-rollingupdate-bf5568d75-qrk6n    0/1     ImagePullBackOff   0          3m55s
   ```
   - Clean Up. Delete the deployment 
    ```    
      kubectl delete deployment/redis-app-rollingupdate
    ```

# Step 3 - Implementing Canary Release Strategy

A canary deployment is an upgraded version of an existing deployment, with all the required application code and dependencies. It is used to test out new features and upgrades to see how they handle the production environment.

When you add the canary deployment to a Kubernetes cluster, it is managed by a service through selectors and labels. The service routes traffic to the pods that have the specified label. This allows you to add or remove deployments easily.

The amount of traffic that the canary gets corresponds to the number of pods it spins up. In most cases, you start by routing a smaller percentage of traffic to the canary and increase the number over time.

With both deployments set up, you monitor the canary behavior to see whether any issues arise. Once you are happy with the way it is handling requests, you can upgrade all the deployments to the latest version.

Note: Canary deployments got their name from an old British mining practice. Back in the day, miners used canaries to test coal mines' safety before they went in. If the canaries returned unharmed, the miners felt safe to enter. However, if something did happen to the birds, they knew that the mines were filled with toxic gases.

## Setting up Canary Deployment on Kubernetes
The steps below show you how to set up a canary deployment. For this lab, we created a simple Kubernetes cluster of Nginx pods with a basic, two-sentence static HTML page. The versions of the deployment vary by the content they display on the web page.

The process of setting up your canary deployment will differ according to the application you are running.

- Create the deployment definition using a yaml file. Use a text editor of your choice and provide a name for the file. We are going to name the file `nginx-deployment.yaml` :

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
 name: nginx
spec:
 selector:
  matchLabels:
   app: nginx
 replicas: 3
 template:
  metadata:
   labels:
    app: nginx
    version: "1.0"
  spec:
   containers:
    - name: nginx
      image: nginx:alpine
      resources:
      limits:
       memory: "128Mi"
       cpu: "50m"
      ports:
      - containerPort: 80
      volumeMounts:
      - mountPath: /usr/share/nginx/html
        name: index.html
      volumes:
      - name: index.html
        hostPath:
          path: /C/Temp/v1
```
We created 3 replicas of Nginx pods for the Kubernetes cluster. All the pods have the label version: "1.0". Additionally, they have a host volume containing the `index.html` mounted to the container. The sample HTML file consisting of:

```html
<html>
<h1>Hello World!</h1>
<p>This is version 1</p>
</html>
```

- Create the deployment by running:

```shell
kubectl apply -f nginx-deployment.yaml

```
- Create the Service
The next step is to create a service definition for the Kubernetes cluster. The service will route requests to the specified pods.
```yaml
apiVersion: v1
kind: Service
metadata:
 name: nginx-service
spec:
 type: LoadBalancer
selector:
 app: nginx
 version: "1.0"
ports:
- port: 8888
  targetPort: 80
```
The yaml file specifies the type of service – LoadBalancer. It instructs the service to balance workloads between pods with the labels `app: nginx` and `version: "1.0"`. The pod needs to have both labels to be part of the service.

```shell
kubectl apply -f nginx-deployment.service.yaml
```
Check First Version of Cluster: To verify the service is running, open a web browser, and navigate to the IP and port number defined in the service file.

To see the external IP address of the service, use the command:
```
kubectl get service
```
If you are running Kubernetes locally, use localhost as the IP.

Since the sample cluster we created is running locally on port 8888, the URL is:<http://localhost:8888>

The browser should display a Hello World message from version 1.


- Create a Canary Deployment

With version 1 of the application in place, you deploy version 2, the canary deployment.

Start by creating the yaml file for the canary deployment. 

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
 name: nginx-canary-deployment
spec:
 selector:
  matchLabels:
   app: nginx
 replicas: 3
 template:
  metadata:
   labels:
    app: nginx
    version: "2.0"
  spec:
   containers:
    - name: nginx
      image: nginx:alpine
      resources:
      limits:
       memory: "128Mi"
       cpu: "50m"
      ports:
      - containerPort: 80
      volumeMounts:
      - mountPath: /usr/share/nginx/html
        name: index.html
      volumes:
      - name: index.html
        hostPath:
          path: /C/temp/v2
```
The content of the canary deployment file differs by three important parameters:

The name in the metadata is `nginx-canary-deployment`.
It has the label version: `"2.0"`.
It is linked to a html file `index.html` which consists of:
```html
<html>
<h1>Hello World!</h1>
<p>This is version 2</p>
</html>
```
## Testing the Canary deployment
- Create the canary deployment with the command:

```shell
kubectl apply -f nginx-canary-deployment.yaml
```

Verify you have successfully deployed the three additional pods:

Run the Canary Deployment
Open a web browser and navigate to the same IP address with the previous deployment. You will notice there are no changes to the web page. This is because the service file is configured to load balance only pods with the label version: "1.0".

To test out the updated pods, you need to modify the service file and direct part of the traffic to version: “2.0”.

Open the file `nginx-deployment.service.yaml`. Find and remove the line version: “1.0”. The file should include the following:

```yaml
apiVersion: v1
kind: Service
metadata:
 name: nginx-service
spec:
 type: LoadBalancer
selector:
 app: nginx
ports:
- port: 8888
  targetPort: 80

```
Create the updated service with the command:

```
kubectl apply -f nginx-deployment.service.yml
```
The traffic is now split between version 1 and version 2 pods. If you refresh the web page a few times, you see different results depending on where the service redirects your request.

_Routing traffic to canary deployment_
Note: In the example above, half of the traffic is redirected to the canary deployment. This is because there are three replicas of version 1 and three replicas of version 2. However, you can start off by redirecting a smaller percentage of the requests. Simply configure the canary deployment to have fewer pods. You can gradually increase the number of replicas once you are confident the canary can handle more traffic.

- Monitor the Canary Behavior
With both deployments up and running, monitor the behavior of the new deployment. Depending on the results, you can roll back the deployment or upgrade to the newer version.

Roll Back Canary Deployment
If you notice the canary is not performing as expected, you can roll back the deployment and delete the upgraded pods with:

```shell
kubectl delete deployment/nginx-canary-deployment
```
The service continues load balancing the traffic to the initial (version 1) pods.

- Roll Out Upgraded Deployment

If you conclude the canary deployment is performing as expected, you can route all incoming traffic to the upgraded version. There are three ways to do so:
  1. Upgrade the first version by modifying the Docker image and building a new deployment. Then, remove the canaries with:
   ```shell
   kubectl delete deployment/nginx-canary-deployment
   ```
  2. You can keep the upgraded pods and remove the ones with the version 1 label:
  ```
  kubectl delete deployment/nginx
  ```
  3.  Alternatively, you can even modify the service.yaml file and add the version specifier to the selector label. This instructs the load balancer to only route traffic to version 2 pods.
   
  # Step 4 - Horizontal Pod Autoscaler Walkthrough

Horizontal Pod Autoscaler automatically scales the number of pods in a replication controller, deployment, replica set or stateful set based on observed CPU utilization (or, with beta support, on some other, application-provided metrics).

This step walks you through an example of enabling Horizontal Pod Autoscaler for an application php-apache server. _metrics-server monitoring needs to be deployed in the cluster_ to provide metrics via the resource metrics API, as Horizontal Pod Autoscaler uses this API to collect metrics. 

- Installing Metrics Server

    

- Run & expose php-apache server
  First, we will start a deployment running the image and expose it as a service using the following configuration: (`unit3-07-horizontal-pod-autoscaler.yaml`)
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: php-apache
  spec:
    selector:
      matchLabels:
        run: php-apache
    replicas: 1
    template:
      metadata:
        labels:
          run: php-apache
      spec:
        containers:
        - name: php-apache
          image: k8s.gcr.io/hpa-example
          ports:
          - containerPort: 80
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
  ---
  apiVersion: v1
  kind: Service
  metadata:
    name: php-apache
    labels:
      run: php-apache
  spec:
    ports:
    - port: 80
    selector:
      run: php-apache
  ```
  - Apply the application and service.
    ```shell
    kubectl apply -f unit4-04-horizontal-pod-autoscaler.yaml
    ```
- Create Horizontal Pod Autoscaler    
   Now that the server is running, we will create the autoscaler using kubectl autoscale. The following command will create a Horizontal Pod Autoscaler that maintains between 1 and 10 replicas of the Pods controlled by the php-apache deployment we created in the first step of these instructions. Roughly speaking, HPA will increase and decrease the number of replicas (via the deployment) to maintain an average CPU utilization across all Pods of 50% (since each pod requests 200 milli-cores by kubectl run), this means average CPU usage of 100 milli-cores).
  ```shell
  kubectl autoscale deployment php-apache --cpu-percent=50 --min=1 --max=10
  ```
  We may check the current status of autoscaler by running:
  ```shell
  kubectl get hpa
  NAME         REFERENCE               TARGETS      MINPODS   MAXPODS   REPLICAS   AGE  
  php-apache   Deployment/php-apache    0%/50%      1         10        0          9s 
  ```
  Please note that the current CPU consumption is 0% as we are not sending any requests to the server (the TARGET column shows the average across all the pods controlled by the corresponding deployment).
- Increase load
   
   Now, we will see how the autoscaler reacts to increased load. We will start a container, and send an infinite loop of queries to the php-apache service (please run it in a different terminal):
  ```shell
  kubectl run -it --rm load-generator --image=k8s.gcr.io/busybox
  ```
  Hit enter for command prompt:
  ```shell
  while true; do wget -q -O- http://php-apache; done
  ```
  Within a minute or so, we should see the higher CPU load by executing:

  ```shell
  kubectl get hpa  
  NAME         REFERENCE               TARGETS    MINPODS   MAXPODS   REPLICAS   AGE
  php-apache   Deployment/php-apache   248%/50%   1         10        1          6m24s
  ```
  Here, CPU consumption has increased to 248% of the request. As a result, the deployment was resized to 5 replicas:
  ```shell
  kubectl get deployment php-apache
  NAME         READY   UP-TO-DATE   AVAILABLE   AGE
  php-apache   5/5     5            5           8m7s
  ```
    > Note: It may take a few minutes to stabilize the number of replicas. Since the amount of load is not controlled in any way it may happen that the final number of replicas will differ from this example.
- Stop load
     
  We will finish our example by stopping the user load.
  In the terminal where we created the container with busybox image, terminate the load generation by typing <Ctrl> + C.
  Then we will verify the result state (after a minute or so):
  ```shell
  kubectl get hpa
  NAME         REFERENCE                     TARGET       MINPODS   MAXPODS   REPLICAS   AGE
  php-apache   Deployment/php-apache/scale   0% / 50%     1         10        1          11m
  ```
  ```shell
  kubectl get deployment php-apache
  NAME         READY   UP-TO-DATE   AVAILABLE   AGE
  php-apache   1/1     1            1           27m
  ```
  Here CPU utilization dropped to 0, and so HPA autoscaled the number of replicas back down to 1.

  > Note: Autoscaling the replicas may take a few minutes
- Clean Up
     
  Remove the deployment and the service
  ```shell 
  kubectl delete -f unit4-04-horizontal-pod-autoscaler.yaml
  ```  
  Remove the HPA autoscaler
  ```shell 
  kubectl delete hpa php-apache
  ```  