package org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mpls.label.rev150504;

import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mpls.label.rev150504.MplsLabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.topology.mpls.label.rev150504.MplsLabelReservedType;

/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class MplsLabelTypeBuilder {

    public static MplsLabelType getDefaultInstance(java.lang.String defaultValue) {
        try {
            MplsLabelReservedType reserved = new MplsLabelReservedType(Long.valueOf(defaultValue));
            return new MplsLabelType(reserved);
        } catch (final IllegalArgumentException e) {
            return new MplsLabelType(Long.valueOf(defaultValue));
        }
    }

}
