# Lab 03- Configuration
---

## Lab Steps

# Step 1 - ConfigMaps and Secrets
The 3rd factor (Configuration) of the [Twelve-Factor App principles](https://12factor.net/) states:

  > Configuration that varies between deployments should be stored in the environment.

This is where Kubernetes ConfigMaps and Secrets can help by supplying your deployment containers with the contextual and secretive information they require. Secrets and ConfigMaps behave similarly in Kubernetes, both in how they are created and because they can be exposed inside a container as mounted files or volumes or environment variables.

In the following steps you will learn:

  - how to create configuration data in the form of ConfigMaps and Secrets,
  - how Pods make configuration accessible for applications in containers,

### Creating ConfigMaps
A ConfigMap is simple data associated with a unique key. They can be created and shared in the containers in the same ways as secrets. ConfigMaps are intended for non-sensitive data—configuration data—like config files and environment variables and are a great way to create customized running services from generic container images.
- **Create ConfigMaps from litteral values**
  
  You can write a `YAML` representation of the ConfigMap manually and load it into Kubernetes, or you can use the CLI  `kubectl create configmap` command to create it from the command line. 
  - **Create ConfigMap from CLI**
  
   The following example creates a ConfigMap using the CLI.
    ```shell
    kubectl create configmap mysql-config --from-literal=DB_NAME="ProductsDB" --from-literal=USER_NAME="kubernetes"
    ```
     Key/value configs are passed using the `--from-literal` option. It is possible to declare more than one `--from-literal` in order to pass multiple configuration entries in the same command. 
     
    - Check Kubernetes using `kubectl get configmap `after the create. 
    ```shell
      kubectl get configmap mysql-config 
    ```
    - To see the actual data, get it in YAML form.
    ```shell
      kubectl get configmap mysql-config -o yaml
    ```
    - Or, in description form
    ```shell
      kubectl describe configmap mysql-config 
    ```
    - Finally, to clean up delete the configmap.
    ```shell
      kubectl delete configmap mysql-config
    ```
  - **Create ConfigMap from YAML**
  
   A better way to define ConfigMaps is with a resource YAML file in this form.

      - Edit a yaml file named `unit3-01-configmaps.yaml` and initialize it as follows.
      ```yaml
      apiVersion: v1
      kind: ConfigMap
      metadata:
        name: mysql-config-yaml
        namespace: default
      data:
        DB_NAME: ConfluenceDB
        USER_NAME: kubernetes
        confluence.cnf: |
          [mysqld]
            collation-server=utf8_bin
            default-storage-engine=INNODB 
            max_allowed_packet=256M           
      ``` 
    - Create the ConfigMap YAML resource using the following command
      ```shell
      kubectl create -f unit3-01-configmaps.yaml
      ```
    - Then, view it.

      ```shell
      kubectl describe configmap mysql-config-yaml
      ```
    The same ConfigMaps can also be explored in the Kubernetes Dashboard.
    - Clean up. Remove the configMap using `kubectl delete configmap command`.

      ```shell
      kubectl delete configmap mysql-config-yaml
      ```

- **Create ConfigMaps from files**
 
 You can also create ConfigMaps from from a file. To do this use the `--from-file` option of the `kubectl create configmap` command.  Your file should have a set of `key=value` pairs, one per line. If a value spans over more than one line, rely on backslash + end-of-line to escape the end of line. 
  - View the content of the given properties file which is located in th `config` folder `unit3-01-mysql.properties`
    ```yaml
    DB_NAME=ConfluenceDB
    USER_NAME=kubernetes 
    ```
  - Execute the following to create a ConfigMap from that configuration file:
    ```shell
    kubectl create configmap mysql-config-from-file --from-file=configs/unit3-01-mysql.properties
    ```
  - Now that we've created the ConfigMap, we can view it with:
    ```shell
    kubectl get configmaps
    ```
    We can get an even better view with:
    ```shell
    kubectl get configmap mysql-config-from-file -o yaml
    ```
  - Clean Up. Remove the ConfigMap using the following command:
    ```shell
    kubectl delete configmap mysql-config-from-file
    ```
  > **Note**: Kubernetes does not have know-how of how to ignore the **comments** and **blank lines** if we use `--from-file`. The `--from-env-file` option fixes that automatically. 
  It is a good practice to have your ConfigMaps in environment file format and use `--from-env-file` to create your ConfigMaps. 

### Accessing ConfigMaps
Once the configuration data is stored in ConfigMaps, the containers can access the data. Pods grant their containers access to the ConfigMaps through these three techniques:
   1. through the application command-line arguments,
   2. through the system environment variables accessible by the application,
   3. through a specific read-only file accessible by the application.

Let's explore these access techniques.

- **Accessing ConfigMaps through Command Line Arguments**
   
   This example shows how a Pod accesses configuration data from the ConfigMap by passing in the data through the command-line arguments when running the container. Upon startup, the application would reference these parameters from the program's command-line arguments.

  - Let's create a ConfigMap resource definition
   ```shell
    kubectl create configmap mysql-config-cli --from-env-file=configs/unit3-01-mysql.properties
   ```
  - Create the following Pod Definition. Name the Yaml file `unit3-01-configmaps-inject-cli.yaml` and set it as follows.
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: inject-config-via-cli
    spec:
      containers:
        - name: consuming-container
          image: k8s.gcr.io/busybox
          command: [ "/bin/sh", "-c", "echo $(PROPERTY_DB_NAME); echo $(PROPERTY_DB_USER); env" ]
          env:        
            - name: PROPERTY_DB_NAME
              valueFrom:
                configMapKeyRef:
                  name: mysql-config-cli
                  key: DB_NAME
            - name: PROPERTY_DB_USER
              valueFrom:
                configMapKeyRef:
                  name: mysql-config-cli
                  key: USER_NAME
          restartPolicy: Never
      ```  
  Run the following command to create the Pod
  ```shell
  kubectl apply -f unit3-01-configmaps-inject-cli.yaml
  ```
  - Inspect the log of the Pod to verify that the configuration has been applied.
  ```shell
  kubectl logs inject-config-via-cli
  ```
  - Clean Up. Remove the ConfigMap and the Pod.
  ```shell
  kubectl delete configmap mysql-config-cli 
  kubectl delete pod inject-config-via-cli
  ```
- **Accessing ConfigMaps Through Environment variables**
   
   This example shows how a Pod accesses configuration data from the ConfigMap by passing in the data as environmental parameters of the container. Upon startup, the application would reference these parameters as system environment variables.

  - Let's create a ConfigMap resource definition
   ```shell
    kubectl create configmap mysql-config-env --from-env-file=configs/unit3-01-mysql.properties
   ```
  - Create the following Pod Definition. Name the Yaml file `unit3-01-configmaps-inject-env.yaml` and set it as follows.
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: inject-config-via-env
    spec:
      containers:
        - name: consuming-container
          image: k8s.gcr.io/busybox      
          command: [ "/bin/sh", "-c", "env" ]
          envFrom:      
          - configMapRef:
              name: mysql-config-env
      restartPolicy: Never
    ```  
  Run the following command to create the Pod
  ```shell
  kubectl apply -f unit3-01-configmaps-inject-env.yaml
  ```
  - Inspect the log of the Pod to verify that the configuration has been applied.
  ```shell
  kubectl logs inject-config-via-env
  ```
  - Clean Up. Remove the ConfigMap and the Pod.
  ```shell
  kubectl delete configmap mysql-config-env 
  kubectl delete pod inject-config-via-env
  ```
- **Accessing ConfigMaps Through Volume Mounts**
   
   This example shows how a Pod accesses configuration data from the ConfigMap by reading from a file in a directory of the container. Upon startup, the application would reference these parameters by referencing the named files in the known directory.

  - Let's create a ConfigMap resource definition
   ```shell
    kubectl create configmap mysql-config-volume --from-env-file=configs/unit3-01-mysql.properties
   ```
  - Create the following Pod Definition. Name the Yaml file `unit3-01-configmaps-inject-volume.yaml` and set it as follows.
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: inject-config-via-volume
    spec:
      containers:
        - name: consuming-container
          image: k8s.gcr.io/busybox      
          command: [ "/bin/sh","-c","cat /etc/config/keys" ]
          volumeMounts:      
          - name: config-volume
            mountPath: /etc/config
      volumes:
        - name: config-volume
          configMap:
            name: mysql-config-volume
            items:
            - key: DB_NAME
              path: keys
      restartPolicy: Never
     ```  
  Run the following command to create the Pod
  ```shell
  kubectl apply -f unit3-01-configmaps-inject-volume.yaml
  ```
  - Inspect the log of the Pod to verify that the configuration has been applied.
  ```shell
  kubectl logs inject-config-via-volume
  ```
  - Clean Up. Remove the ConfigMap and the Pod.
  ```shell
  kubectl delete configmap mysql-config-volume 
  kubectl delete pod inject-config-via-volume
  ```

### Creating Secrets

Secrets are Kubernetes objects intended for storing a small amount of sensitive data. It is worth noting that Secrets are stored base64-encoded within Kubernetes, so they are not wildly secure. Make sure to have appropriate role-based access controls (RBAC) to protect access to Secrets. Even so, extremely sensitive Secrets data should probably be stored using something like HashiCorp Vault.
Both ConfigMaps and Secrets are stored in etcd, but the way you submit secrets is slightly different than ConfigMaps.

- **Create Secrets from CLI**
  
  To create secrets you can use the CLI  `kubectl create secret` command or you can write a `YAML` representation of the Secret manually and load it into Kubernetes.

 To create the Secret you should convert it to base64. To do this, there are many possiblities like the `base64` Unix Command, the `System.Convert` Utility in Pwershell, on you can simply use one of the free online base64 encoders.

    _Encoding/Decoding into base64 in Bash Shell_
    ```shell
    $ echo -n 'KubernetesRocks!' | base64    # For Encoding
      S3ViZXJuZXRlc1JvY2tzIQ==
    $ echo "TXlEYlBhc3N3MHJkCg==" | base64 --decode   # For Decoding
      KubernetesRocks!
    ```
    _Encoding/Decoding into base64 in Windows PowerShell_
    ```shell
    PS> [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("KubernetesRocks!"))
        S3ViZXJuZXRlc1JvY2tzIQ==
    PS> [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("S3ViZXJuZXRlc1JvY2tzIQ=="))
       KubernetesRocks!
    ```  
  - Let's create a secret using the encoded `base64` value of '`KubernetesRocks!`';
     ```shell
      kubectl create secret generic db-password --from-literal=password=S3ViZXJuZXRlc1JvY2tzIQ==
     ```
  - Check Kubernetes using `kubectl get secret `after the create. 
      ```shell
        kubectl get secret db-password 
      ```
  - To see the actual data, get it in YAML form.
  ```shell
    kubectl get secret db-password -o yaml
  ```
  - To decode the secret in Bash Shell
  ```shell
    kubectl get secret db-password -o 'go-template={{index .data "password"}}' | base64 --decode
  ```    
  - Finally, to clean up delete the secret.
  ```shell
    kubectl delete secret db-password
  ```
    > Note : Never confuse encoding with encryption, as they are two very different concepts. Values encoded are not encrypted. The encoding is to allow a wider variety of values for secrets. You can easily decode the text with a simple base64 command to reveal the original password text.

- **Create Secrets from YAML**
  
 A better way to define Secrets is with a resource YAML file in this form.
 - Create the following Secret YAML definition file. Name it `unit3-01-secrets.yaml` and set it as follows:
    ```yaml
    apiVersion: v1
    kind: Secret
    metadata:
      name: mysql-secrets
    type: Opaque
    data:
      user-password: a3ViZXJuZXRlcw==       #kubernetes
      root-password: S3ViZXJuZXRlc1JvY2tzIQ==   # KubernetesRocks!
    ```
  Run the following command to create the Secret resource.
  ```shell
  kubectl apply -f unit3-01-secrets.yaml
  ```
 - Check Kubernetes using `kubectl get secret `after the create. 
    ```shell
      kubectl get secret mysql-secrets 
    ```
 - To decode the secret in Bash Shell
    ```shell
      kubectl get secret mysql-secrets -o 'go-template={{index .data "password"}}' | base64 --decode
    ```    
 - Finally, to clean up delete the secret.
  ```shell
    kubectl delete secret mysql-secrets
  ```
   **Hint** :  When first creating the YAML file you can skip using the base64 command and instead use the kubectl `--dry-run` feature which will generate the YAML file for you with the encoding.
    ```
    kubectl create secret generic db-password --from-literal=password=MyDbPassw0rd --dry-run -o yaml > my-secret.yaml
    ```
    If you view the content of `my-secret.yaml` you will see the base64 encoded value of the password.

# Step 2 - Configuring the Spring Boot applications

The folder `01-Hardcoded-config` contains a version of the application where the configuration variables are hard coded. Deploy the application and check it works.

The objective of this step is to produce a new version of the application where the configuration will be externalised in ConfigMap and secret objets. We have to update the MySQL deployment as well as the Spring Boot Api deployment.

## Externalize the MySQL Configuration to ConfigMaps and Secrets

Let's review the hard coded MySQL deployment. It is loocated in this file `01-Hardcoded-config\bookstore-backend-with-mysql\K8s\backend-mysql.yaml`. We have to send the `MYSQL_DATABASE` value to a configMap and the `MYSQL_ROOT_PASSWORD` and `MYSQL_PASSWORD` values to secrets.

  ```yaml
  apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-mysql
  labels:
    app: mysql  
spec:
  replicas: 1
  selector:
    matchLabels:
        app: bookstore
        tier: backend-mysql
  template:
    metadata:
      labels:
        app: bookstore
        tier: backend-mysql
    spec:
      containers:
      - name: mysql
        image: quay.io/mromdhani/mysql:8.0
        env:
          - name: MYSQL_DATABASE
            value: bookstoredb
          - name: MYSQL_ROOT_PASSWORD
            value: password 
          - name: MYSQL_PASSWORD
            value: password         
        ports:
        - containerPort: 3306
          protocol: TCP
  ```
- Elaborate a new version of the MySQL deployement where configuration variables are provided in ConfigMaps and Secrets. Make use of the following artifacts.
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: backend-mysql-configs
  namespace: default
data:
  MYSQL_DATABASE: bookstoredb
```
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: backend-mysql-secrets
type: Opaque
data:
  MYSQL_ROOT_PASSWORD: cGFzc3dvcmQ=
  MYSQL_PASSWORD: cGFzc3dvcmQ=  
```

## Externalize the SpringBoot REST Api Configuration to ConfigMaps and Secrets

Let's review the hard coded SpringBoot REST Api deployment. It is loocated in ths file `01-Hardcoded-config\bookstore-backend-with-mysql\K8s\backend-api.yaml`. We have to send the  `MYSQL_SERVER`, the `MYSQL_DATABASE`, the `MYSQL_PORT` values to a configMap and the `MYSQL_PASSWORD` values to a secret object.
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-api
spec:
  replicas: 1
  selector:
    matchLabels:
      app: bookstore
      tier: backend-api
  template:
    metadata:
      labels:
        app: bookstore
        tier: backend-api
    spec:
      containers:
        - name: backend
          image: "quay.io/mromdhani/bookstore-backend:v1"
          ports:
          - containerPort: 8080
          env:
            - name: MYSQL_SERVER
              value: backend-mysql
            - name: MYSQL_DATABASE
              value: bookstoredb
            - name: MYSQL_PORT
              value: '3306'
            - name: MYSQL_PASSWORD
              value: password
```
- Elaborate a new version of the Spring Boot REST Api deployement where configuration variables are provided in ConfigMaps and Secrets. Make use of the following artifacts.
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: backend-api-configs
  namespace: default
data:
  MYSQL_SERVER: backend-mysql
  MYSQL_DATABASE: bookstoredb
  MYSQL_PORT : '3306'
```
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: backend-api-secrets
type: Opaque
data:
  MYSQL_PASSWORD: cGFzc3dvcmQ= 
```

**Task:** Deploy the new version of the backend (REST Api + MySQL) and check it works.

# Step 3 - Configuring Local Volumes : EmptyDir and HostPath

Volumes are used by Pods to persist data. In this step you will practice the configuration of the two types of local volumes: **EmptyDir** and **HostPath**. Each of these volume has its own use case and should be used only in those specific cases. There are more than 20 volume types Kubernetes supports: [Kubernetes Volume Types](https://kubernetes.io/docs/concepts/storage/volumes/).

### emptyDir

An `emptyDir` volume is first created when a Pod is assigned to a Node, and **exists as long as that Pod is running on that node**.
As the name says, it is initially empty. All Containers in the same Pod can read and write in the same emptyDir volume.
When a Pod is restarted or removed, the data in the emptyDir is lost forever.

Some use cases for an `emptyDir` are:

- scratch space, such as for a disk-based merge sort
- checkpointing a long computation for recovery from crashes
- holding files (caching) that a content-manager container fetches while a webserver container serves the data

The storage media (Disk, SSD, etc.) of an emptyDir volume is determined by the medium of the filesystem holding the kubelet root dir (typically `/var/lib/kubelet`). You can set the `emptyDir.medium` field to `"Memory"` to tell Kubernetes to mount a tmpfs (RAM-backed filesystem) for you instead.  

- View the YAML file named `unit3-02-emptydir.yaml`. It describes a Pod using an `emptyDir` for Caching.
  ```yaml
  apiVersion: v1
  kind: Pod
  metadata:
    name: test-pd
  spec:
    containers:
    - image: gcr.io/google_containers/test-webserver
      name: test-container
      volumeMounts:
      - mountPath: /cache
        name: cache-volume
    volumes:
    - name: cache-volume
      emptyDir: {}
  ```

### hostPath

A `hostPath` volume mounts a file or directory from **the host node's filesystem** into your pod. This is not something that most Pods will need, but it offers a powerful escape hatch for some applications.

For example, some uses for a `hostPath` are:

- running a container that needs access to Docker internals; use a hostPath of `/var/lib/docker`
- running `cAdvisor` in a container; use a `hostPath` of `/dev/cgroups`

_Task:_

  - Consider the Pod definition given in the YAML file `unit3-02-hostpath-starter.yaml`.The Pod uses an `emptyDir`. You are asked to change this volume into a `hostPath` volume named `hp-volume`. The Path in the host should be `/data` and the mount path within the container should be `/data/test`. 

# Step 4 - Persistent Volumes (PVs) and Persistent Volume Claims (PVCs)

Kubernetes persistent volumes (PVs) are user-provisioned storage volumes assigned to a Kubernetes cluster. Persistent volumes' life-cycle is independent from any pod using it. Thus, persistent volumes are perfect for use cases in which you need to retain data regardless of the unpredictable life process of Kubernetes pods.

Persistent Volume Claims (PVCs) are objects that connect to back-end storage volumes through a series of abstractions. A PersistentVolumeClaim is a request for a resource with specific attributes, such as storage size. In between the two is a process that matches a claim to an available volume and binds them together. This allows the claim to be used as a volume in a pod.

### Creating a PersistentVolume

PersistentVolumes abstract the low-level details of a storage device, and provide a high-level API to provide such storage to Pods.

PersistentVolumes are storage inside of your cluster that has been provisioned by your administrator. Their lifecycle is external to your Pods or other objects.

There are many different types of PersistentVolumes that can be used with Kubernetes. As an example, you can use a **local filesystem**, **NFS**, and there are plugins for **cloud vendor storage solutions** like EBS.

- Let's specify PersistentVolumes via a Manifest file (`unit3-03-persistentvolume.yaml`):

  ```yaml
  apiVersion: v1
  kind: PersistentVolume
  metadata:
    name: local-pv
  spec:
    capacity:
      storage: 500Mi
    volumeMode: Filesystem
    accessModes:
    - ReadWriteOnce
    persistentVolumeReclaimPolicy: Delete
    storageClassName: local-storage
    hostPath:
      path: "/mnt/data"
  ```
  This describes a single PersistentVolume. It is mounted to `/mnt/data` on a node. It is of type `Filesystem`, with `500 MB` of storage. (`hostPath` are only appropriate for testing in single node environments)

  - We can create this PersistentVolume:
    ```shell
    kubectl apply -f unit3-03-persistentvolume.yaml
    ```
  - We can then view it with:
  ```shell
  kubectl get pv
  ```
  - We can get even more information with:
  ```shell
  kubectl describe pv local-pv
  ```

### Creating a PersistentVolumeClaim

Now that we have a PersistentVolume, let's make a PersistentVolumeClaim to provide storage to a Pod. PersistentVolumeClaims enable you to request a certain amount of storage from a PersistentVolume, and reserve it for your Pod.

The following is a YAML manifest for a PersistentVolumeClaim (`unit3-03-persistentvolumeclaim.yaml`):
  ```yaml
  apiVersion: v1
  kind: PersistentVolumeClaim
  metadata:
    name: nginx-pvc
  spec:
    # Notice the storage-class name matches the storage class in the PV we made in the previous step.
    storageClassName: local-storage
    accessModes:
      - ReadWriteOnce
    resources:
      requests:
        storage: 20Mi
  ```      
  This PersistentVolumeClaim is requesting `20 MB` of storage from a local Filesystem PersistentVolume. When a Pod uses this Claim, Kubernetes will attempt to satisfy the claim by enumerating all PersistentVolumes, and matching the requirements in this Claim to what is present in the cluster.

  If we were to match this Claim to PersistentVolume, it would succeed, because we have a PersistentVolume of type Filesystem with 100 GB of storage.

  - Let's create the PersistentVolumeClaim:
  ```shell
  kubectl apply -f unit3-03-persistentvolumeclaim.yaml
  ```
  - and wait until the resource is available:
  ```shell
  kubectl get pvc --watch
  ```
  - We can also use label selectors to aid in matching Claims with PersistentVolumes.
  ```yaml
  apiVersion: v1
  kind: PersistentVolumeClaim
  metadata:
    name: nginx-pvc
  spec:
    accessModes:
      - ReadWriteOnce
    resources:
      requests:
        storage: 20Mi
    selector:
      matchLabels:
        env: dev
  ```
  This Claim is identical to the previous one, but it will only be matched with PersistentVolumes that have the label `env: dev`. You can use this to have more control over which Claims bind to a particular PersistentVolume.

### Adding Storage to Pods

Now that we have PersistentVolumes and a PersistentVolumeClaim, we can provide the Claim to a Pod, and Kubernetes will provision storage.
The following YAMLmanifest (`unit3-03-pod-with-pvc.yaml`) describes a Pod using a PVC.
  ```yaml
  apiVersion: v1
  kind: Pod
  metadata:
    name: nginx
  spec:
    containers:
    - name: nginx
      image: quay.io/mromdhani/nginx:latest
      volumeMounts:
      - name: nginx-data
        mountPath: /data/nginx
    volumes:
    - name: nginx-data
      persistentVolumeClaim:
        claimName: nginx-pvc
  ```      
  This is very similar to a Pod that uses a local storage. Now, we have basically changed what provides the Storage with the addition of those bottom two lines. 
  - To deploy our pod, execute the following:
  ```shell
  kubectl apply -f unit3-03-pod-with-pvc.yaml
  ```
  - We can see that the Pod was created, and that the Claim was fulfilled:
  ```shell
  kubectl get pods --watch
  kubectl get pvc
  ```
  - Clean Up. Remove the PV, the PVC and the Pod
   ```shell
  kubectl delete pv/local-pv, pvc/nginx-pvc, po/nginx 
  ```

### BookStoreBackend: Refactor the MySQL tier to use a PersistentVolume

Back to our BookStore Back, the goal here is to refactor the MySQL tier in order to use a Persistent Volume.
Here is the definition of an example of PersistentVolume and an example of PersistentVolumeClaim. Refactor the YAML manifest of the MySQL Pod to use these definitions. 

   - Indication : The solution is provided in this folder `03-Config-with-ConfigMaps-Persistent-Volume\bookstore-backend-with-mysql\K8s`.

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: mysql-pv-volume
  labels:
    type: local
spec:
  storageClassName: manual
  capacity:
    storage: 50Mi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  hostPath:
    path: "/mnt/data"
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pv-claim
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 50Mi
```
# Step 5 - Managing Resources for Containers and Pods
By default, containers run with unbounded compute resources on a Kubernetes cluster. With resource quotas, cluster administrators can restrict resource consumption and creation on a namespace basis.  

As a cluster administrator, you might want to impose restrictions on resources that Pods can use. When you specify a Pod, you can optionally specify how much of each resource a Container needs. The most common resources to specify are CPU and memory (RAM). These resources play a critical role in how the scheduler allocate pods to nodes.
Kubernetes uses the requests & limits structure to control resources such as CPU and memory.
  - **Requests** are what the container is guaranteed to get. For example, If a container requests a resource, Kubernetes will only schedule it on a node that can give it that resource.
  - **Limits**, on the other hand, is the resource threshold a container never exceed. The container is only allowed to go up to the limit, and then it is restricted.

Let's specify resources for the ngix container. For example, we specify a request of `0.25 cpu` and `64MiB` (64* 2 exp(20) bytes) of memory and a limit of `0.5 cpu` and `256MiB` of memory.

- Create a Deployment by editing a new YAML file. Name it `unit3-04-limit-resources.yaml`. Initialize it with the following content :

  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: nginx-app-limits
    labels:
      app: nginx-app-limits
  spec:
    replicas: 3
    selector:
      matchLabels:
        app: nginx-app-limits
    template:
      metadata:
        labels:
          app : nginx-app-limits
      spec:
        containers:
          - name: nginx
            image: quay.io/mromdhani/nginx
            resources:
              requests: # Initial requests
                cpu: "250m" # Equivalent to 0.25 CPU (Quater CPU)
                memory: "64Mi" # Equivalent to 64 * 2 of power(20)             
              limits:  # Hard limits
                cpu: "500m"
                memory: "256Mi"              
  ```
- Check that these resource limits are taken into account. Get the list of pods using `kubectl get pods`. Then, get the detailed descrition of one of the pods using the `kubectl describe pod [pod_name]`.  
- Clean Up. Delete the Deployment using the `kubectl delete deployment nginx-app-limits`
- Create a **LimitRange** and a Pod
There is a concern that one Pod or Container could monopolize all available resources. A LimitRange is a policy to constrain resource allocations (to Pods or Containers) in a namespace.
  - Create a namespace called `constraints-mem-example`
```shell
  kubectl create namespace constraints-mem-example
```
  - Create a LimitRange specification in a file named `unit3-04-limit-range.yaml`.
    ```yaml
    apiVersion: v1
    kind: LimitRange
    metadata:
      name: mem-min-max
    spec:
      limits:
      - max:
          memory: 1Gi
        min:
          memory: 500Mi
        type: Container
    ```
  - Apply the LimitRange to the namespace `constraints-mem-example`:
    ```shell
      kubectl apply -f unit3-04-limit-range.yaml --namespace=constraints-mem-example  
    ```
  - View detailed information about the LimitRange:
    ```
      kubectl get limitrange mem-min-max --namespace=constraints-mem-example --output=yaml
    ```
- Attempt to create a Pod that exceeds the maximum memory constraint
  - Create a new Yaml manifest for a Pod that has one Container. The Container specifies a memory request of `800 MiB` and a memory limit of `1.5 GiB`.  Named it `unit3-04-pod-exceeding-range.yaml` and initialize it as follows:
    ```yaml
    apiVersion: v1
    kind: Pod
    metadata:
      name: constraints-mem-exceeding
    spec:
      containers:
      - name: constraints-mem-exceeding
        image: quay.io/mromdhani/nginx
        resources:
          limits:
            memory: "1.5Gi"
          requests:
            memory: "800Mi"
    ```
  - Attempt to create the Pod:  
  ```shell
  kubectl apply -f unit3-04-pod-exceeding-range.yaml --namespace=constraints-mem-example
  ```
  The output shows that the Pod does not get created, because the Container specifies a memory limit that is too large:
  ```shell
  Error from server (Forbidden): error when creating "unit3-04-pod-exceeding-range.yaml": pods "constraints-mem-exceeding" is forbidden: maximum memory usage per Container is 1Gi, 
  but limit is 1536Mi
  ```
- Clean Up. Delete the the Pod, The LimiRange, and the NameSpace objects.
  ```
  kubectl delete pod constraints-mem-exceeding  --namespace=constraints-mem-example
  kubectl delete limitrange mem-min-max --namespace=constraints-mem-example
  kubectl delete ns constraints-mem-example
  ```
# Step 6 - Configuring CPU and Memory Resources for the BookStore Backend application.

To set the requests and limits values for our BookStore Backend application, we have to run the application for some time and send some load to it. Then, we can use the statistical measurements provided by **Metrics Server** tool. Think that the data provided is an approximation, you can customize it. 

- Enable Metrics Server Addons on Minikibe using the following command. The command should be issued from an Administrative console.
```
minikube addons enable metrics-server 
```
- Start the application the application
```
kubectl apply -f ./K8s   
```

- Send some HTTP Requests the API Endpoint
```
 curl -X GET http://<clusterip>:8080/books 
 curl -X GET http://<clusterip>:8080/books/1 
 curl -X GET http://<clusterip>:8080/categories 
```

- Get the statistics for Pods:
```
$ kubectl top pods
```

- Update the manifests of the Spring Boot Api and the MySQL server by the resources data.
  An example of data, may look like this:

   - In Spring Boot API Container Spec:
```yaml
          resources:
            requests:
              memory: "300Mi"
              cpu: "100m"
            limits:
              memory: "800Mi"
              cpu: "500m"  
```
   - In MySQL Container Spec:
```yaml
          resources:
            requests:
              memory: "350Mi"
              cpu: "50m"
            limits:
              memory: "900Mi"
              cpu: "500m"  
```