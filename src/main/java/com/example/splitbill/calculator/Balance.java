package com.example.splitbill.calculator;

import java.math.BigDecimal;

import com.example.splitbill.model.Participant;

public record Balance(Participant participant, BigDecimal netBalance) {

}
