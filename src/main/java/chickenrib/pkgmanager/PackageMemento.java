package chickenrib.pkgmanager;

import java.util.List;

import chickenrib.pkgmanager.action.InstallAction;
import suite.pkgmanager.PackageManifest;

public class PackageMemento {

	private PackageManifest packageManifest;
	private List<InstallAction> installActions;

	public PackageMemento() { // for de-serialization
	}

	public PackageMemento(PackageManifest packageManifest, List<InstallAction> installActions) {
		this.packageManifest = packageManifest;
		this.installActions = installActions;
	}

	public PackageManifest getPackageManifest() {
		return packageManifest;
	}

	public List<InstallAction> getInstallActions() {
		return installActions;
	}

}
