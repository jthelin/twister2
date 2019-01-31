//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.rsched.schedulers.k8s;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.squareup.okhttp.Response;

import edu.iu.dsc.tws.common.resource.NodeInfoUtils;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.rsched.utils.ProcessUtils;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1beta2Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ConfigMapList;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeAddress;
import io.kubernetes.client.models.V1NodeList;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1SecretList;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceList;
import io.kubernetes.client.models.V1beta2StatefulSet;
import io.kubernetes.client.models.V1beta2StatefulSetList;

/**
 * a controller class to talk to the Kubernetes Master to manage jobs
 */

public class KubernetesController {
  private static final Logger LOG = Logger.getLogger(KubernetesController.class.getName());

  private String namespace;
  private String jobName;
  private String cmName;

  private ApiClient apiClient = null;
  private CoreV1Api coreApi;
  private AppsV1beta2Api appsApi;

  public KubernetesController(String namespace, String jobName) {
    this.namespace = namespace;
    this.jobName = jobName;
    this.cmName = KubernetesUtils.createConfigMapName(jobName);
  }

  public void initialize() throws IOException {
    apiClient = io.kubernetes.client.util.Config.defaultClient();
    Configuration.setDefaultApiClient(apiClient);
    coreApi = new CoreV1Api(apiClient);
  }


  /**
   * return the StatefulSet object if it exists in the Kubernetes master,
   * otherwise return null
   */
  public boolean existStatefulSets(List<String> statefulSetNames) {

    if (appsApi == null) {
      appsApi = new AppsV1beta2Api(apiClient);
    }

    V1beta2StatefulSetList setList = null;
    try {
      setList = appsApi.listNamespacedStatefulSet(
          namespace, null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting StatefulSet list.", e);
      throw new RuntimeException(e);
    }

    for (V1beta2StatefulSet statefulSet : setList.getItems()) {
      if (statefulSetNames.contains(statefulSet.getMetadata().getName())) {
        LOG.severe("There is already a StatefulSet with the name: "
            + statefulSet.getMetadata().getName());
        return true;
      }
    }

    return false;
  }

  /**
   * return the StatefulSet object if it exists in the Kubernetes master,
   * otherwise return null
   */
  public boolean existStatefulSet(String statefulSetName) {

    if (appsApi == null) {
      appsApi = new AppsV1beta2Api(apiClient);
    }

    V1beta2StatefulSetList setList = null;
    try {
      setList = appsApi.listNamespacedStatefulSet(
          namespace, null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting StatefulSet list.", e);
      throw new RuntimeException(e);
    }

    for (V1beta2StatefulSet statefulSet : setList.getItems()) {
      if (statefulSetName.equals(statefulSet.getMetadata().getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * return the list of StatefulSet names that matches this jobs StatefulSet names for workers
   * they must be in the form of "jobName-index"
   * otherwise return an empty ArrayList
   */
  public ArrayList<String> getStatefulSetsForJobWorkers() {

    if (appsApi == null) {
      appsApi = new AppsV1beta2Api(apiClient);
    }

    V1beta2StatefulSetList setList = null;
    try {
      setList = appsApi.listNamespacedStatefulSet(
          namespace, null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting StatefulSet list.", e);
      throw new RuntimeException(e);
    }

    ArrayList<String> ssNameList = new ArrayList<>();

    for (V1beta2StatefulSet statefulSet : setList.getItems()) {
      String ssName = statefulSet.getMetadata().getName();
      if (ssName.matches(jobName + "-" + "[0-9]+")) {
        ssNameList.add(ssName);
      }
    }

    return ssNameList;
  }

  /**
   * create the given StatefulSet on Kubernetes master
   */
  public boolean createStatefulSet(V1beta2StatefulSet statefulSet) {

    if (appsApi == null) {
      appsApi = new AppsV1beta2Api(apiClient);
    }

    String statefulSetName = statefulSet.getMetadata().getName();
    try {
      Response response = appsApi.createNamespacedStatefulSetCall(
          namespace, statefulSet, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "StatefulSet [" + statefulSetName + "] is created.");
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when creating the StatefulSet [" + statefulSetName + "]: "
            + response);
        LOG.log(Level.SEVERE, "Submitted StatefulSet Object: " + statefulSet);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when creating the StatefulSet: " + statefulSetName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when creating the StatefulSet: " + statefulSetName, e);
    }
    return false;
  }

  /**
   * delete the given StatefulSet from Kubernetes master
   */
  public boolean deleteStatefulSet(String statefulSetName) {

    if (appsApi == null) {
      appsApi = new AppsV1beta2Api(apiClient);
    }

    try {
      V1DeleteOptions deleteOptions = new V1DeleteOptions();
      deleteOptions.setGracePeriodSeconds(0L);
      deleteOptions.setPropagationPolicy(KubernetesConstants.DELETE_OPTIONS_PROPAGATION_POLICY);

      Response response = appsApi.deleteNamespacedStatefulSetCall(
          statefulSetName, namespace, deleteOptions, null, null, null, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "StatefulSet [" + statefulSetName + "] is deleted.");
        return true;

      } else {

        if (response.code() == 404 && response.message().equals("Not Found")) {
          LOG.log(Level.SEVERE, "There is no StatefulSet [" + statefulSetName
              + "] to delete on Kubernetes master. It may have already terminated.");
          return true;
        }

        LOG.log(Level.SEVERE, "Error when deleting the StatefulSet ["
            + statefulSetName + "]: " + response);
        return false;
      }

    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the StatefulSet: " + statefulSetName, e);
      return false;
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the StatefulSet: " + statefulSetName, e);
      return false;
    }
  }

  /**
   * scale up or down the given StatefulSet
   */
  public boolean patchStatefulSet(String ssName, int replicas) {

    if (appsApi == null) {
      appsApi = new AppsV1beta2Api(apiClient);
    }

    String jsonPatchStr =
        "{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":" + replicas + "}";
    Object obj = (new Gson()).fromJson(jsonPatchStr, JsonElement.class);
    ArrayList<Object> objectList = new ArrayList<>();
    objectList.add(obj);

    try {
      Response response = appsApi.patchNamespacedStatefulSetCall(
          ssName, namespace, objectList, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "StatefulSet [" + ssName + "] is patched.");
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when patching the StatefulSet [" + ssName + "]: "
            + response);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when patching the StatefulSet: " + ssName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when patching the StatefulSet: " + ssName, e);
    }
    return false;
  }

  /**
   * create the given service on Kubernetes master
   */
  public boolean createService(V1Service service) {

    String serviceName = service.getMetadata().getName();
    try {
      Response response = coreApi.createNamespacedServiceCall(
          namespace, service, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "Service [" + serviceName + "] created.");
        return true;
      } else {
        LOG.log(Level.SEVERE, "Error when creating the service [" + serviceName + "]: " + response);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when creating the service: " + serviceName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when creating the service: " + serviceName, e);
    }
    return false;
  }

  /**
   * return true if one of the services exist in Kubernetes master,
   * otherwise return false
   */
  public boolean existServices(List<String> serviceNames) {
// sending the request with label does not work for list services call
//    String label = "app=" + serviceLabel;
    V1ServiceList serviceList = null;
    try {
      serviceList = coreApi.listNamespacedService(namespace,
          null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting service list.", e);
      throw new RuntimeException(e);
    }

    for (V1Service service : serviceList.getItems()) {
      if (serviceNames.contains(service.getMetadata().getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * return true if the given service exist in Kubernetes master,
   * otherwise return false
   */
  public boolean existService(String serviceName) {
// sending the request with label does not work for list services call
//    String label = "app=" + serviceLabel;
    V1ServiceList serviceList = null;
    try {
      serviceList = coreApi.listNamespacedService(namespace,
          null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting service list.", e);
      throw new RuntimeException(e);
    }

    for (V1Service service : serviceList.getItems()) {
      if (serviceName.equals(service.getMetadata().getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * delete the given service from Kubernetes master
   */
  public boolean deleteService(String serviceName) {

    V1DeleteOptions deleteOptions = new V1DeleteOptions();
    deleteOptions.setGracePeriodSeconds(0L);
    deleteOptions.setPropagationPolicy(KubernetesConstants.DELETE_OPTIONS_PROPAGATION_POLICY);

    try {
      Response response = coreApi.deleteNamespacedServiceCall(
          serviceName, namespace, deleteOptions, null, null, null, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.info("Service [" + serviceName + "] is deleted.");
        return true;

      } else {

        if (response.code() == 404 && response.message().equals("Not Found")) {
          LOG.warning("There is no Service [" + serviceName
              + "] to delete on Kubernetes master. It may have already been terminated.");
          return true;
        }

        LOG.severe("Error when deleting the Service [" + serviceName + "]: " + response);
        return false;
      }
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the service: " + serviceName, e);
      return false;
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the service: " + serviceName, e);
      return false;
    }
  }

  /**
   * sending a command to shell
   */
  public static boolean runProcess(String[] command) {
    StringBuilder stderr = new StringBuilder();
    int status =
        ProcessUtils.runSyncProcess(false, command, stderr, new File("."), false);

//    if (status != 0) {
//      LOG.severe(String.format(
//          "Failed to run process. Command=%s, STDOUT=%s, STDERR=%s", command, stdout, stderr));
//    }
    return status == 0;
  }

  /**
   * check whether the given PersistentVolumeClaim exist on Kubernetes master
   * @param pvcName
   * @return
   */
  public boolean existPersistentVolumeClaim(String pvcName) {
    V1PersistentVolumeClaimList pvcList = null;
    try {
      pvcList = coreApi.listNamespacedPersistentVolumeClaim(
          namespace, null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting PersistentVolumeClaim list.", e);
      throw new RuntimeException(e);
    }

    for (V1PersistentVolumeClaim pvc : pvcList.getItems()) {
      if (pvcName.equals(pvc.getMetadata().getName())) {
        LOG.severe("There is already a PersistentVolumeClaim with the name: " + pvcName);
        return true;
      }
    }

    return false;
  }

  /**
   * create the given PersistentVolumeClaim on Kubernetes master
   */
  public boolean createPersistentVolumeClaim(V1PersistentVolumeClaim pvc) {

    String pvcName = pvc.getMetadata().getName();
    try {
      Response response = coreApi.createNamespacedPersistentVolumeClaimCall(
          namespace, pvc, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "PersistentVolumeClaim [" + pvcName + "] is created.");
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when creating the PersistentVolumeClaim [" + pvcName
            + "] Response: " + response);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when creating the PersistentVolumeClaim: " + pvcName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when creating the PersistentVolumeClaim: " + pvcName, e);
    }
    return false;
  }

  public boolean deletePersistentVolumeClaim(String pvcName) {

    try {
      Response response = coreApi.deleteNamespacedPersistentVolumeClaimCall(
          pvcName, namespace, null, null, null, null, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "PersistentVolumeClaim [" + pvcName + "] is deleted.");
        return true;

      } else {

        if (response.code() == 404 && response.message().equals("Not Found")) {
          LOG.log(Level.WARNING, "There is no PersistentVolumeClaim [" + pvcName
              + "] to delete on Kubernetes master. It may have already been deleted.");
          return true;
        }

        LOG.log(Level.SEVERE, "Error when deleting the PersistentVolumeClaim [" + pvcName
            + "] Response: " + response);
        return false;
      }
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the PersistentVolumeClaim: " + pvcName, e);
      return false;
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the PersistentVolumeClaim: " + pvcName, e);
      return false;
    }
  }

  /**
   * return true if the Secret object with that name exists in Kubernetes master,
   * otherwise return false
   */
  public boolean existSecret(String secretName) {
    V1SecretList secretList = null;
    try {
      secretList = coreApi.listNamespacedSecret(namespace,
          null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting Secret list.", e);
      throw new RuntimeException(e);
    }

    for (V1Secret secret : secretList.getItems()) {
      if (secretName.equalsIgnoreCase(secret.getMetadata().getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * get NodeInfoUtils objects for the nodes on this cluster
   * @return the NodeInfoUtils object list. If it can not get the list from K8s master, return null.
   */
  public ArrayList<JobMasterAPI.NodeInfo> getNodeInfo(String rackLabelKey,
                                                      String datacenterLabelKey) {

    V1NodeList nodeList = null;
    try {
      nodeList = coreApi.listNode(null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting NodeList.", e);
      return null;
    }

    ArrayList<JobMasterAPI.NodeInfo> nodeInfoList = new ArrayList<>();
    for (V1Node node : nodeList.getItems()) {
      List<V1NodeAddress> addressList = node.getStatus().getAddresses();
      for (V1NodeAddress nodeAddress: addressList) {
        if ("InternalIP".equalsIgnoreCase(nodeAddress.getType())) {
          String nodeIP = nodeAddress.getAddress();
          String rackName = null;
          String datacenterName = null;

          // get labels
          Map<String, String> labelMap = node.getMetadata().getLabels();
          for (String key: labelMap.keySet()) {
            if (key.equalsIgnoreCase(rackLabelKey)) {
              rackName = labelMap.get(key);
            }
            if (key.equalsIgnoreCase(datacenterLabelKey)) {
              datacenterName = labelMap.get(key);
            }
          }

          JobMasterAPI.NodeInfo nodeInfo =
              NodeInfoUtils.createNodeInfo(nodeIP, rackName, datacenterName);
          nodeInfoList.add(nodeInfo);
          break;
        }
      }
    }

    return nodeInfoList;
  }

  /**
   * create the given ConfigMap on Kubernetes master
   */
  public boolean createConfigMap(V1ConfigMap configMap) {

    String configMapName = configMap.getMetadata().getName();
    try {
      Response response = coreApi.createNamespacedConfigMapCall(
          namespace, configMap, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "ConfigMap [" + configMapName + "] is created.");
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when creating the ConfigMap [" + configMapName + "]: "
            + response);
        LOG.log(Level.SEVERE, "Submitted ConfigMap Object: " + configMap);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when creating the ConfigMap: " + configMapName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when creating the ConfigMap: " + configMapName, e);
    }
    return false;
  }

  /**
   * delete the given ConfigMap from Kubernetes master
   */
  public boolean deleteConfigMap(String configMapName) {

    V1DeleteOptions deleteOptions = new V1DeleteOptions();
    deleteOptions.setGracePeriodSeconds(0L);
    deleteOptions.setPropagationPolicy(KubernetesConstants.DELETE_OPTIONS_PROPAGATION_POLICY);

    try {
      Response response = coreApi.deleteNamespacedConfigMapCall(
          configMapName, namespace, deleteOptions, null, null, null, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.info("ConfigMap [" + configMapName + "] is deleted.");
        return true;

      } else {

        if (response.code() == 404 && response.message().equals("Not Found")) {
          LOG.warning("There is no ConfigMap [" + configMapName
              + "] to delete on Kubernetes master. It may have already been deleted.");
          return true;
        }

        LOG.severe("Error when deleting the ConfigMap [" + configMapName + "]: " + response);
        return false;
      }
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the ConfigMap: " + configMapName, e);
      return false;
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when deleting the ConfigMap: " + configMapName, e);
      return false;
    }
  }

  /**
   * return true if there is already a ConfigMap object with the same name on Kubernetes master,
   * otherwise return false
   */
  public boolean existConfigMap(String configMapName) {
    V1ConfigMapList configMapList = null;
    try {
      configMapList = coreApi.listNamespacedConfigMap(namespace,
          null, null, null, null, null, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting ConfigMap list.", e);
      throw new RuntimeException(e);
    }

    for (V1ConfigMap configMap : configMapList.getItems()) {
      if (configMapName.equals(configMap.getMetadata().getName())) {
        LOG.severe("There is already a ConfigMap with the name: " + configMapName);
        return true;
      }
    }

    return false;
  }

  /**
   * return start count for the given workerID
   * if there is no key for the given workerID, return -1
   */
  public int getStartCount(int workerID) {
    String paramName = "START_COUNT_FOR_WORKER_" + workerID;

    String jobLabel = KubernetesUtils.createJobPodsLabelWithKey(jobName);

    V1ConfigMapList configMapList = null;
    try {
      configMapList = coreApi.listNamespacedConfigMap(
          namespace, null, null, null, null, jobLabel, null, null, null, null);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when getting ConfigMap list.", e);
      throw new RuntimeException(e);
    }

    for (V1ConfigMap configMap : configMapList.getItems()) {
      if (cmName.equals(configMap.getMetadata().getName())) {
        Map<String, String> pairs = configMap.getData();
        if (pairs == null) {
          return -1;
        }

        String countStr = pairs.get(paramName);
        if (countStr == null) {
          return -1;
        } else {
          return Integer.parseInt(countStr);
        }
      }
    }

    LOG.severe("There is no ConfigMap for the job: " + jobName);
    return -1;
  }


  /**
   * update a start count in the job ConfigMap
   */
  public boolean updateStartCount(int workerID, int startCount) {

    String paramName = "START_COUNT_FOR_WORKER_" + workerID;
    String countStr = "\"" + startCount + "\"";

    String jsonPatchStr =
        "{\"op\":\"replace\",\"path\":\"/data/" + paramName + "\",\"value\":" + countStr + "}";
    Object obj = (new Gson()).fromJson(jsonPatchStr, JsonElement.class);
    ArrayList<Object> objectList = new ArrayList<>();
    objectList.add(obj);

    try {
      Response response = coreApi.patchNamespacedConfigMapCall(
          cmName, namespace, objectList, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "ConfigMap parameter updated " + paramName + " = " + startCount);
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when patching the ConfigMap [" + cmName + "]: "
            + response);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when patching the ConfigMap: " + cmName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when patching the ConfigMap: " + cmName, e);
    }
    return false;
  }

  /**
   * add a new start count to the job ConfigMap
   */
  public boolean addStartCount(int workerID, int startCount) {

    String paramName = "START_COUNT_FOR_WORKER_" + workerID;
    String countStr = "\"" + startCount + "\"";

    String jsonPatchStr =
        "{\"op\":\"add\",\"path\":\"/data/" + paramName + "\",\"value\":" + countStr + "}";
    Object obj = (new Gson()).fromJson(jsonPatchStr, JsonElement.class);
    ArrayList<Object> objectList = new ArrayList<>();
    objectList.add(obj);

    try {
      Response response = coreApi.patchNamespacedConfigMapCall(
          cmName, namespace, objectList, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "ConfigMap parameter added " + paramName + " = " + startCount);
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when patching the ConfigMap [" + cmName + "]: "
            + response);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when patching the ConfigMap: " + cmName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when patching the ConfigMap: " + cmName, e);
    }
    return false;
  }

  /**
   * add a new start count to the job ConfigMap
   */
  public boolean removeStartCount(int workerID) {

    String paramName = "START_COUNT_FOR_WORKER_" + workerID;

    String jsonPatchStr =
        "{\"op\":\"remove\",\"path\":\"/data/" + paramName + "\"}";
    Object obj = (new Gson()).fromJson(jsonPatchStr, JsonElement.class);
    ArrayList<Object> objectList = new ArrayList<>();
    objectList.add(obj);

    try {
      Response response = coreApi.patchNamespacedConfigMapCall(
          cmName, namespace, objectList, null, null, null).execute();

      if (response.isSuccessful()) {
        LOG.log(Level.INFO, "ConfigMap parameter removed " + paramName);
        return true;

      } else {
        LOG.log(Level.SEVERE, "Error when patching the ConfigMap [" + cmName + "]: "
            + response);
        return false;
      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when patching the ConfigMap: " + cmName, e);
    } catch (ApiException e) {
      LOG.log(Level.SEVERE, "Exception when patching the ConfigMap: " + cmName, e);
    }
    return false;
  }

}
