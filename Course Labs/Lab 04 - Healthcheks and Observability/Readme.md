# Lab 02- Healthchecks and Observability
---

- [Step 1 - Readiness Probe](#step-1---readiness-probe)
- [Step 2 - Liveness Probe](#step-2---liveness-probe)
- [Step 3 - Startup Probe](#step-3---startup-probe)
- [step 4 - Do you Need Liveness and Readiness Probes](#step-4---do-you-need-liveness-and-readiness-probes)
- [Step 5 - Debugging in Kubernetes](#step-5---debugging-in-kubernetes)
  - [Inspecting events](#inspecting-events)
  - [Inspecting logs](#inspecting-logs)
  - [Opening an Interactive Shell](#opening-an-interactive-shell)



# Step 1 - Readiness Probe

In this step, we define a readiness probe for a Node.js application. The Node.js application exposes an HTTP endpoint on the root context path and runs on port 3000. Dealing with a web-based application makes an HTTP GET request a perfect fit for probing its readiness. 

In the YAML manifest shown below, the readiness probe executes its first check after two seconds and repeats checking every eight seconds thereafter. All other attributes use the default values. A readiness probe will continue to periodically check, even after the application has been successfully started.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: readiness-pod
spec:
  containers:
  - image: bmuschko/nodejs-hello-world:1.0.0
    name: hello-world
    ports:
    - name: nodejs-port
      containerPort: 3000
    readinessProbe:
      httpGet:
        path: /
        port: nodejs-port
      initialDelaySeconds: 2
      periodSeconds: 8
```

- Create a Pod by pointing the create command to the YAML manifest. During the Pod's startup process, it's very possible that the status shows Running but the container isn't ready to accept incoming requests yet, as indicated by 0/1 under the READY column:

```shell
$ kubectl create -f readiness-probe.yaml
pod/readiness-pod created
$ kubectl get pod readiness-pod
NAME                READY   STATUS    RESTARTS   AGE
pod/readiness-pod   0/1     Running   0          6s
$ kubectl get pod readiness-pod
NAME                READY   STATUS    RESTARTS   AGE
pod/readiness-pod   1/1     Running   0          68s
$ kubectl describe pod readiness-pod
...
Containers:
  hello-world:
    ...
    Readiness:      http-get http://:nodejs-port/ delay=2s timeout=1s \
                    period=8s #success=1 #failure=3
```

# Step 2 - Liveness Probe

A liveness probe checks if the application is still working as expected down the road. For the purpose of demonstrating a liveness probe, we'll use a custom command. A custom command is probably the most flexible way to verify the health of a container, as it allows for calling any command available to the container. That can either be a command-line tool that comes with the base image or a tool that you install as part of the containization process.

We'll have the application create and update a file, `/tmp/heartbeat.txt`, to show that it's still alive. We'll do this by it run the Unix touch command every five seconds. The probe will periodically check if the modification timestamp of the file is older than one minute. If it is, then Kubernetes can assume that the application isn't functioning as expected and will restart the container.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: liveness-pod
spec:
  containers:
  - image: busybox
    name: app
    args:
    - /bin/sh
    - -c
    - 'while true; do touch /tmp/heartbeat.txt; sleep 5; done;'
    livenessProbe:
      exec:
        command:
        - test `find /tmp/heartbeat.txt -mmin -1`
      initialDelaySeconds: 5
      periodSeconds: 30
```

The following command uses the YAML manifest stored in the file `liveness-probe.yaml` to create the Pod. Describing the Pod renders information on the liveness probe. Not only can we inspect the custom command and its configuration, we can also see how many times the container has been restarted upon a probing failure:

```shell
$ kubectl create -f liveness-probe.yaml
pod/liveness-pod created
$ kubectl get pod liveness-pod
NAME               READY   STATUS    RESTARTS   AGE
pod/liveness-pod   1/1     Running   0          22s
$ kubectl describe pod liveness-pod
...
Containers:
  app:
    ...
    Restart Count:  0
    Liveness:       exec [test `find /tmp/heartbeat.txt -mmin -1`] delay=5s \
                    timeout=1s period=30s #success=1 #failure=3
...
```

# Step 3 - Startup Probe

The purpose of a startup probe is to figure out when an application is fully started. Defining the probe is especially useful for an application that takes a long time to start up. The kubelet puts the readiness and liveness probes on hold while the startup probe is running. A startup probe finishes its operation under one of the following conditions:
 - If it could verify that the application has been started.
 - If the application doesn’t respond within the timeout period.

To demonstrate the functionality of the startup probe, Example below defines a Pod that runs the Apache HTTP server in a container. By default, the image exposes the container port 80, and that’s what we’re probing for using a TCP socket connection.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: startup-pod
spec:
  containers:
  - image: httpd:2.4.46
    name: http-server
    startupProbe:
      tcpSocket:
        port: 80
      initialDelaySeconds: 3
      periodSeconds: 15
```
As you can see in the following terminal output, the describe command can retrieve the configuration of a startup probe as well:

```shell
$ kubectl create -f startup-probe.yaml
pod/startup-pod created
$ kubectl get pod startup-pod
NAME              READY   STATUS    RESTARTS   AGE
pod/startup-pod   1/1     Running   0          31s
$ kubectl describe pod startup-pod
...
Containers:
  http-server:
     ...
     Startup:        tcp-socket :80 delay=3s timeout=1s period=15s \
                     #success=1 #failure=3
...
```

# step 4 - Do you Need Liveness and Readiness Probes

This [article](https://developers.redhat.com/blog/2020/11/10/you-probably-need-liveness-and-readiness-probes/), the author presents some case studies discusses the use of  Liveness and Readiness Probes. The examples presented are the following:

- Example 1: A static file server (Nginx)
- Example 2: A jobs server (no REST API)
- Example 3: A server-side rendered application with an API
- Example 4: Putting it all together

Walk throug these examples and assess the propositions of the authors.   

# Step 5 - Debugging in Kubernetes

When operating an application in a production Kubernetes cluster, it’s almost inevitable that you’ll come across failure situations. You can’t completely leave this job up to the Kubernetes adminstrator—it’s your responsibility as an application developer to be able to troubleshoot issues for the Kubernetes objects you designed and deployed.

## Inspecting events

It’s totally possible that you’ll not encounter any of those error statuses. But there’s still a chance of the Pod having a configuration issue. You can retrieve detailed information about the Pod and its events using the kubectl describe pod command to inspect its events. The following output belongs to a Pod that tries to mount a Secret that doesn’t exist. Instead of rendering a specific error message, the Pod gets stuck with the status ContainerCreating:

```shell
$ kubectl get pods
NAME         READY   STATUS              RESTARTS   AGE
secret-pod   0/1     ContainerCreating   0          4m57s
$ kubectl describe pod secret-pod
...
```
```shell
$ kubectl get events
LAST SEEN   TYPE      REASON            OBJECT               MESSAGE
3m14s       Warning   BackOff           pod/custom-cmd       Back-off restarting \
```

## Inspecting logs

When debugging a Pod, the next level of details can be retrieved by downloading and inspecting its logs. You may or may not find additional information that points to the root cause of a misbehaving Pod. It’s definitely worth a look. The YAML manifest shown below defines a Pod running a shell command.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: incorrect-cmd-pod
spec:
  containers:
  - name: test-container
    image: busybox
    command: ['/bin/sh', '-c', 'unknown']
```
After creating the object, the Pod fails with the status `CrashLoopBackOff`. Running the logs command reveals that the command run in the container has an issue:

```shell
$ kubectl create -f crash-loop-backoff.yaml
pod/incorrect-cmd-pod created
$ kubectl get pods incorrect-cmd-pod
NAME                READY   STATUS              RESTARTS   AGE
incorrect-cmd-pod   0/1     CrashLoopBackOff    5          3m20s
$ kubectl logs incorrect-cmd-pod
/bin/sh: unknown: not found
```
The `logs` command provides two helpful options I’d like to mention here. The option -f streams the logs, meaning you’ll see new log entries as they’re being produced in real time. The option `--previous` gets the logs from the previous instantiation of a container, which is helpful if the container has been restarted.

## Opening an Interactive Shell

If any of the previous commands don’t point you to the root cause of the failing Pod, it’s time to open an interactive shell to a container. As an application developer, you’ll probably know best what behavior to expect from the application at runtime. Ensure that the correct configuration has been created and inspect the running processes by using the Unix or Windows utility tools, depending on the image run in the container.

Say you encounter a situation where a Pod seems to work properly on the surface, as shown in Example below.

A Pod periodically writing the current date to a file
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: failing-pod
spec:
  containers:
  - args:
    - /bin/sh
    - -c
    - while true; do echo $(date) >> ~/tmp/curr-date.txt; sleep \
      5; done;
    image: busybox
    name: failing-pod
```
After creating the Pod, you check the status. It says Running; however, when making a request to the application, the endpoint reports an error. Next, you check the logs. The log output renders an error message that points to a nonexistent directory. Apparently, the directory hasn’t been set up correctly but is needed by the application:

```shell
$ kubectl create -f failing-pod.yaml
pod/failing-pod created
$ kubectl get pods failing-pod
NAME          READY   STATUS    RESTARTS   AGE
failing-pod   1/1     Running   0          5s
$ kubectl logs failing-pod
/bin/sh: can't create /root/tmp/curr-date.txt: nonexistent directory
```

The `exec` command opens an interactive shell to further investigate the issue. Below, we’re using the Unix tools mkdir, cd, and ls inside of the running container to fix the problem. Obviously, the better mitigation strategy is to create the directory from the application or provide an instruction in the Dockerfile:

```shell
$ kubectl exec failing-pod -it -- /bin/sh
# mkdir -p ~/tmp
# cd ~/tmp
# ls -l
total 4
-rw-r--r-- 1 root root 112 May 9 23:52 curr-date.txt
```
