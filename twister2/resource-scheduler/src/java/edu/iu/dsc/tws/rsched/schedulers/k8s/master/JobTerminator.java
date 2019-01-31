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
package edu.iu.dsc.tws.rsched.schedulers.k8s.master;

import java.util.ArrayList;

import edu.iu.dsc.tws.master.IJobTerminator;
import edu.iu.dsc.tws.rsched.schedulers.k8s.KubernetesController;
import edu.iu.dsc.tws.rsched.schedulers.k8s.KubernetesUtils;

public class JobTerminator implements IJobTerminator {

  private KubernetesController controller;
  private String namespace;

  public JobTerminator(KubernetesController controller) {
    this.controller = controller;
  }

  @Override
  public boolean terminateJob(String jobName) {
    // delete the StatefulSets for workers
    ArrayList<String> ssNameLists = controller.getStatefulSetsForJobWorkers();
    boolean ssForWorkersDeleted = true;
    for (String ssName: ssNameLists) {
      ssForWorkersDeleted &= controller.deleteStatefulSet(ssName);
    }

    // delete the job service
    String serviceName = KubernetesUtils.createServiceName(jobName);
    boolean serviceForWorkersDeleted = controller.deleteService(serviceName);

    // delete the job master service
    // if Job Master runs in client, no service is started
    // so, there may not be a service for job aster
    String jobMasterServiceName = KubernetesUtils.createJobMasterServiceName(jobName);
    boolean serviceForJobMasterDeleted = true;
    if (controller.existService(jobMasterServiceName)) {
      serviceForJobMasterDeleted = controller.deleteService(jobMasterServiceName);
    }

    // last delete the job master StatefulSet
    // if Job Master runs in client, no statefulset is started for job master
    // first check whether there is a ss created
    String jobMasterStatefulSetName = KubernetesUtils.createJobMasterStatefulSetName(jobName);
    boolean ssForJobMasterDeleted = true;
    if (controller.existStatefulSet(jobMasterStatefulSetName)) {
      ssForJobMasterDeleted = controller.deleteStatefulSet(jobMasterStatefulSetName);
    }

    // delete the persistent volume claim
    // peristent volume is optional
    // first check whether there is a pvc created
    String pvcName = KubernetesUtils.createPersistentVolumeClaimName(jobName);
    boolean pvcDeleted = true;
    if (controller.existPersistentVolumeClaim(pvcName)) {
      pvcDeleted = controller.deletePersistentVolumeClaim(pvcName);
    }

    String cmName = KubernetesUtils.createConfigMapName(jobName);
    boolean cmDeleted = controller.deleteConfigMap(cmName);

    return ssForWorkersDeleted
        && serviceForWorkersDeleted
        && serviceForJobMasterDeleted
        && pvcDeleted
        && ssForJobMasterDeleted
        && cmDeleted;
  }
}
