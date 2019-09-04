package org.jenkinsci.plugins.configfiles.folder;

import org.jenkinsci.plugins.configfiles.ConfigByIdComparator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * This class is required for XStream backward compatibility because {@link FolderConfigFileProperty}
 * formerly contained an anonymous inner class defining the comparator.
 *
 * For old XML configurations that still contain a comparator element as follows
 * {@code <comparator class="org.jenkinsci.plugins.configfiles.folder.FolderConfigFileProperty$1"/>}
 * XStream tries to resolve a class named exactly like this during deserialization.
 *
 * @deprecated replaced by {@link ConfigByIdComparator}
 */
@Deprecated
@Restricted(DoNotUse.class)
final class FolderConfigFileProperty$1 extends ConfigByIdComparator {}
