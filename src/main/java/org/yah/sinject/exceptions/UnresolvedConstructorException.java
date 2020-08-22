package org.yah.sinject.exceptions;


import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.impl.builder.declarations.CandidateConstuctors;

public class UnresolvedConstructorException extends ServiceResolutionException {

    private final CandidateConstuctors<?> candidates;

    public UnresolvedConstructorException(ServiceDeclaration<?> declaration, CandidateConstuctors<?> candidates) {
        super(declaration.name(), declaration.type(), "unresolved constructor");
        this.candidates = candidates;
    }

    public CandidateConstuctors<?> getCandidates() {
        return candidates;
    }

}
