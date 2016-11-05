package com.github.zabbicook.operations

import com.github.zabbicook.entity.Entity.{NotStored, Stored}
import com.github.zabbicook.entity.EntityId.{NotStoredId, StoredId}
import com.github.zabbicook.entity.Version
import com.github.zabbicook.entity.action.OperationEvalType.AndOr
import com.github.zabbicook.entity.action.OperationType.sendMessage
import com.github.zabbicook.entity.action._
import com.github.zabbicook.entity.prop.IntProp
import com.github.zabbicook.test.{TestActions, UnitSpec}

/**
  * Created by ryo_natori on 2016/10/28.
  */
class ActionOpSpec extends UnitSpec with TestActions {
  "ActionOperation#isSame" should "return false if not same." in  {
    val a = ActionOperation[NotStored](NotStoredId,sendMessage,None,None,None,None,Some(OperationMessage(Some(true),Some(StoredId("174")),None,None)),None,Some(List(OpMessageUser(StoredId("174")))))
    val b = ActionOperation[Stored](StoredId("159"),sendMessage,Some(IntProp(0)),Some(IntProp(1)),Some(IntProp(1)),Some(AndOr),Some(OperationMessage(Some(true),Some(StoredId("174")),Some(""),Some(""))),Some(List()),Some(List(OpMessageUser(StoredId("174")))))
    assert(b.isSame(a))

    assert(false === b.isSame(a.copy(esc_step_from = Some(2))))

    val a2 = ActionOperation[NotStored](NotStoredId, sendMessage,None,            None,            None,            None,       Some(OperationMessage(Some(false),       Some(StoredId("302")),None,  None)),  None,        Some(List(OpMessageUser(StoredId("293")))))
    val b2 = ActionOperation[Stored](StoredId("431"),sendMessage,Some(IntProp(0)),Some(IntProp(1)),Some(IntProp(1)),Some(AndOr),Some(OperationMessage(Some(false),Some(StoredId("302")),Some(""),Some(""))),Some(List()),Some(List(OpMessageUser(StoredId("293")))))
    assert(b2.isSame(a2))
  }

  withTestOp { (ops, version) =>
    lazy val sut = ops.action

    version + "present actions" should "create, delete, and update actions" in {
      val addedFilter = ActionFilter[NotStored](
        conditions = Seq(
          ActionFilterCondition(
            conditiontype = ActionFilterConditionType.triggerName,
            value = "added",
            operator = Some(FilterConditionOperator.notEqual)
          )
        ),
        evaltype = ActionFilterEvalType.AndOr
      )
      val addedOperations = Seq(
          ActionOperationConfig(
          operationtype = OperationType.sendMessage,
          evaltype = Some(OperationEvalType.AndOr),
          message = Some(OpMessageConfig(
            default_msg = Some(true),
            mediaType = Some(testMediaTypes(0).description)
          )),
          opmessage_grp = Some(Seq(testUserGroups(0).userGroup.name))
        )
      )
      val addedRecoveries = Seq(
        RecoveryActionOperationConfig(
          operationtype = RecoveryOperationType.sendMessage,
          message = Some(OpMessageConfig(
            default_msg = Some(true),
            mediaType = Some(testMediaTypes(1).description)
          )),
          opmessage_grp = Some(Seq(testUserGroups(1).userGroup.name))
        )
      )
      val added = ActionConfig(
          specName("test action xxx"),
          esc_period = 900,
          eventsource = EventSource.trigger,
          def_shortdata = Some("added trigger action"),
          def_longdata = Some("this is long long data"),
          filter = addedFilter,
          operations = addedOperations,
          recoveryOperations = if (version >= Version.of("3.2.0")) Some(addedRecoveries) else None
        )

      def clean() = {
        await(sut.absent(Seq(added.name)))
        cleanTestActions(ops)
      }

      def check(conf: Seq[ActionConfig]) = {
        val founds = await(sut.findByNames(conf.map(_.name)))
        assert(conf.length === founds.length)
        conf.map { expectedSetConf =>
          val actualSet = founds.find(_.action.name == expectedSetConf.name).get
          val expectedSet = await(sut.configToNotStored(expectedSetConf))
          assert(false === actualSet.shouldBeUpdated(expectedSet))
        }
      }

      cleanRun(clean) {
        assert(Seq() === await(sut.findActionsByNames(testActions.map(_.name))))

        // creates
        {
          presentTestActions(ops)
          check(testActions)
        }

        // appends
        {
          val report = await(sut.present(testActions :+ added))
          assert(1 === report.count)
          assert(added.toNotStoredAction.entityName === report.created.head.entityName)
          check(testActions :+ added)
          // represent does nothing
          val report2 = await(sut.present(testActions :+ added))
          assert(report2.isEmpty())
        }

        def checkModified(modified: Seq[ActionConfig]): Unit = {
          val report = await(sut.present(modified))
          assert(1 === report.count)
          assert(modified(0).toNotStoredAction.entityName === report.updated.head.entityName)
          check(modified)
          // represent does nothing
          val report2 = await(sut.present(modified))
          assert(report2.isEmpty())
        }

        // update
        {
          val propUpdated = added.copy(
            def_shortdata = Some("modified trigger action"),
            esc_period = 1000
          )
          checkModified(testActions :+ propUpdated)

          val filterUpdated = added.copy(
            filter = added.filter.copy(
               conditions = Seq(
                 ActionFilterCondition(
                   conditiontype = ActionFilterConditionType.triggerName,
                   value = "modified",
                   operator = Some(FilterConditionOperator.equal)
                 )
               )
            )
          )
          checkModified(testActions :+ filterUpdated)

          val operationsUpdated = added.copy(
            operations = addedOperations.updated(0, addedOperations(0).copy(
              opmessage_grp = None,
              opmessage_usr = Some(Seq(testUsers(0).user.alias))
            ))
          )
          checkModified(testActions :+ operationsUpdated)

          if (added.recoveryOperations.isDefined) {
            val recoveryUpdated = added.copy(
              recoveryOperations = Some(Seq(
                RecoveryActionOperationConfig(
                  operationtype = RecoveryOperationType.sendMessage,
                  message = Some(OpMessageConfig(
                    default_msg = Some(false),
                    mediaType = Some(testMediaTypes(0).description)
                  )),
                  opmessage_usr = Some(Seq(testUsers(0).user.alias))
                )
              ))
            )
            checkModified(testActions :+ recoveryUpdated)
          }
        }
      }
    }
  }
}
