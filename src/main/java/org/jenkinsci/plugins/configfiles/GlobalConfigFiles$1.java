package org.jenkinsci.plugins.configfiles;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * This class is required for XStream backward compatibility because {@link GlobalConfigFiles}
 * formerly contained an anonymous inner class defining the comparator.
 *
 * For old XML configurations that still contain a comparator element as follows
 * {@code <comparator class="org.jenkinsci.plugins.configfiles.GlobalConfigFiles$1"/>}
 * XStream tries to resolve a class named exactly like this during deserialization.
 *
 * @deprecated replaced by {@link ConfigByIdComparator}
 */
@Deprecated
@Restricted(DoNotUse.class)
final class GlobalConfigFiles$1 extends ConfigByIdComparator {}
