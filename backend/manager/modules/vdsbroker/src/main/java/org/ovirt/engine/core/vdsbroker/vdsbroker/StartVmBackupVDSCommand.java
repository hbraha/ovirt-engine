package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.vdscommands.VmBackupVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmCheckpointDao;
import org.ovirt.engine.core.vdsbroker.irsbroker.VmBackupInfo;

public class StartVmBackupVDSCommand<P extends VmBackupVDSParameters> extends VdsBrokerCommand<P> {
    @Inject
    private VmCheckpointDao vmCheckpointDao;

    private Set<Guid> vmCheckpointDisksIds;

    private VmBackupInfo vmBackupInfo;

    public StartVmBackupVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        Guid fromCheckpointId = getParameters().getVmBackup().getFromCheckpointId();
        Guid toCheckpointId = getParameters().getVmBackup().getToCheckpointId();

        Map<String, Object> backupConfig = createBackupConfig(fromCheckpointId, toCheckpointId);
        vmBackupInfo = getBroker().startVmBackup(getParameters().getVmBackup().getVmId().toString(), backupConfig);
        proceedProxyReturnValue();
        setReturnValue(vmBackupInfo);
    }


    @Override
    protected Object getReturnValueFromBroker() {
        return vmBackupInfo;
    }

    @Override
    protected Status getReturnStatus() {
        return vmBackupInfo.getStatus();
    }

    private HashMap[] createDisksMap(Guid toCheckpointId) {
        return getParameters().getVmBackup().getDisks().stream().map(diskImage -> {
            Map<String, Object> imageParams = new HashMap<>();
            imageParams.put(VdsProperties.DomainId, diskImage.getStorageIds().get(0).toString());
            imageParams.put(VdsProperties.ImageId, diskImage.getId().toString());
            imageParams.put(VdsProperties.VolumeId, diskImage.getImageId().toString());
            imageParams.put(VdsProperties.CHECKPOINT, isDiskInCheckpoint(diskImage.getImageId(), toCheckpointId));
            if (diskImage.getBackupMode() != null) {
                imageParams.put(VdsProperties.BACKUP_MODE, diskImage.getBackupMode().getName());
            }
            return imageParams;
        }).toArray(HashMap[]::new);
    }

    private Map<String, Object> createBackupConfig(Guid fromCheckpointId, Guid toCheckpointId) {
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("backup_id", getParameters().getVmBackup().getId().toString());
        backupConfig.put("disks", createDisksMap(toCheckpointId));
        backupConfig.put("from_checkpoint_id", fromCheckpointId != null ? fromCheckpointId.toString() : null);
        backupConfig.put("to_checkpoint_id", toCheckpointId != null ? toCheckpointId.toString() : null);
        backupConfig.put("parent_checkpoint_id", getParentId());
        backupConfig.put("require_consistency", getParameters().isRequireConsistency());

        return backupConfig;
    }

    private boolean isDiskInCheckpoint(Guid diskImageId, Guid toCheckpointId) {
        return toCheckpointId != null && getVmCheckpointDisksIds(toCheckpointId).contains(diskImageId);
    }

    private Set<Guid> getVmCheckpointDisksIds(Guid toCheckpointId) {
        if (vmCheckpointDisksIds == null) {
            vmCheckpointDisksIds = vmCheckpointDao.getDisksByCheckpointId(toCheckpointId).stream()
                    .map(DiskImage::getImageId)
                    .collect(Collectors.toSet());
        }
        return vmCheckpointDisksIds;
    }

    private String getParentId() {
        Guid toCheckpointId = getParameters().getVmBackup().getToCheckpointId();
        if (toCheckpointId != null) {
            Guid parentCheckpointId = vmCheckpointDao.get(toCheckpointId).getParentId();
            return parentCheckpointId != null ? parentCheckpointId.toString() : null;
        }
        return null;
    }
}
