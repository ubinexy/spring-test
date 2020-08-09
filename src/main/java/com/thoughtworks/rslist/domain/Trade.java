package com.thoughtworks.rslist.domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
public class Trade {
    @NotNull
    private BigDecimal amount;
    @NotNull
    @Min(1)
    @Max(5)
    private int rank;
    @NotNull
    private int rsEventId;

    public Trade() {
    }

    public Trade(@NotNull BigDecimal amount, @NotNull @Min(1) int rank, @NotNull int rsEventId) {
        this.amount = amount;
        this.rank = rank;
        this.rsEventId = rsEventId;
    }
}
