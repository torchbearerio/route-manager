package io.torchbearer.routemanager.types

import io.torchbearer.ServiceCore.DataModel.ExecutionPoint

/**
  * Created by fredricvollmer on 2/26/17.
  */
case class InstructionPoint(executionPoint: ExecutionPoint, instruction: Instruction)
