package org.ovirt.engine.ui.uicommonweb.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.ConsoleOptionsFrontendPersister.ConsoleContext;

/**
 * Class that holds (caches) consoles for set of VM (typically contained in some ListModel).
 *
 * It provides a way to get consoles for given vm.
 * The cache must be periodically updated (when the set of VM is updated, the updateVmCache
 * method must be called to ensure the underlying models have fresh and correct instance
 * of their VM).
 *
 */
public class ConsoleModelsCache {

    private final Map<Guid, VmConsoles> vmConsoles;
    private final Map<Guid, VmConsoles> poolConsoles;
    private final Model parentModel;
    private final ConsoleContext consoleContext;

    public ConsoleModelsCache(ConsoleContext consoleContext, Model parentModel) {
        this.consoleContext = consoleContext;
        this.parentModel = parentModel;
        vmConsoles = new HashMap<Guid, VmConsoles>();
        poolConsoles = new HashMap<Guid, VmConsoles>();
    }

    /**
     * Gets consoles for given VM
     * @param entity - VM or VmPool instance
     * @return consoles of given vm
     */
    public VmConsoles getVmConsolesForEntity(Object entity) {
        if (entity instanceof VM) {
            return vmConsoles.get(((VM)entity).getId());
        } else if (entity instanceof VmPool) {
            return poolConsoles.get(((VmPool) entity).getVmPoolId());
        }

        throw new IllegalArgumentException("Entity must be instance of VM or VmPool!"); //$NON-NLS-1$
    }

    /**
     * This must be called every update cycle.
     * @param newItems
     */
    public void updateVmCache(Iterable<VM> newItems) {
        updateCache(true, newItems);
        Set<Guid> vmIds = new HashSet<Guid>();

        if (newItems != null) {
            for (VM vm : newItems) {
                if (vmConsoles.containsKey(vm.getId())) {
                    vmConsoles.get(vm.getId()).setVm(vm); // only update vm
                } else {
                    vmConsoles.put(vm.getId(), new VmConsolesImpl(vm, parentModel, consoleContext));
                }
                vmIds.add(vm.getId());
            }
        }

        vmConsoles.keySet().retainAll(vmIds);
    }

    public void updatePoolCache(Iterable<VM> poolRepresentants) {
        updateCache(false, poolRepresentants);
    }

    /**
     * Updates cache for vms or pool representants (depending on isVmCache param)
     * @param isVmCache - if true, update vm cache based on given entities
     *                  - if false, update pool representants cache.
     * @param entities
     */
    private void updateCache(boolean isVmCache, Iterable<VM> entities) {
        Map<Guid, VmConsoles> cacheToUpdate = isVmCache ? vmConsoles : poolConsoles;
        Set<Guid> entityIdsToKeep = new HashSet<Guid>();

        if (entities != null) {
            for (VM vm : entities) {
                Guid entityKey = isVmCache ? vm.getId() : vm.getVmPoolId();

                if (cacheToUpdate.containsKey(entityKey)) {
                    cacheToUpdate.get(entityKey).setVm(vm); // only update vm
                } else {
                    VmConsoles consoles = isVmCache
                            ? new VmConsolesImpl(vm, parentModel, consoleContext)
                            : new PoolConsolesImpl(vm, parentModel, consoleContext);
                    cacheToUpdate.put(entityKey, consoles);
                }
                entityIdsToKeep.add(entityKey);
            }
        }

        cacheToUpdate.keySet().retainAll(entityIdsToKeep);
    }
}
