/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.topoprocessing.impl.testUtilities;

import com.google.common.base.Optional;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

/**
 * @author martin.uhlir
 *
 */
public class TestDataTreeCandidateNode implements DataTreeCandidateNode {

    private ModificationType modificationType;
    Optional<NormalizedNode<?, ?>> dataAfter;
    private PathArgument pathArgument;

    @Override
    public ModificationType getModificationType() {
        return modificationType;
    }

    public void setModificationType(ModificationType modificationType) {
        this.modificationType = modificationType;
    }

    @Override
    public Collection<DataTreeCandidateNode> getChildNodes() {
        return Collections.emptyList();
    }

    @Override
    public PathArgument getIdentifier() {
        return pathArgument;
    }

    public void setIdentifier(PathArgument pathArgument) {
        this.pathArgument = pathArgument;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataAfter() {
        return dataAfter;
    }

    public void setDataAfter(Optional<NormalizedNode<?, ?>> dataAfter) {
        this.dataAfter = dataAfter;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataBefore() {
        return Optional.absent();
    }

    @Override
    public DataTreeCandidateNode getModifiedChild(final PathArgument identifier) {
        return null;
    }
}
