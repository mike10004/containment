package io.github.mike10004.containment.lifecycle;

/**
 * Exception class for cases where the commission stage of a lifecycle stack failed
 * and one or more exceptions were thrown while unwinding the stack.
 */
class LifecycleStageStackCommissionUnwindException extends LifecycleStageStackCommissionException {

    /**
     * Component lifecycle that threw the exception.
     */
    public final LifecycleStage<?, ?> commissionExceptionThrower;

    /**
     * Exception thrown during the commission stage.
     */
    public final Exception commissionException;

    /**
     * Exception thrown during decommission.
     */
    public final LifecycleStageStackDecommissionException unwindException;

    public LifecycleStageStackCommissionUnwindException(LifecycleStage<?, ?> commissionExceptionThrower, Exception commissionException, LifecycleStageStackDecommissionException unwindException) {
        super(String.format("commission failed and %d exceptions were thrown while unwinding", unwindException.exceptionsThrown.size()));
        this.commissionExceptionThrower = commissionExceptionThrower;
        this.commissionException = commissionException;
        this.unwindException = unwindException;
    }
}
