package ${package};

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author zhangxingrui
 * @create 2018-08-13 14:06
 **/
public class TestMain {

    private static final Logger logger = LoggerFactory.getLogger(TestMain.class);

    public static void main(String[] args) throws ParseException {
        logger.info("启动我们的程序");

        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        //处理流程任务
        processTask(processEngine, processInstance);

        logger.info("结束我们的程序");

    }

    /**
     * @Author: xingrui
     * @Description: 处理流程任务
     * @Date: 14:26 2018/8/13
     */
    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            logger.info("待处理任务数量 [{}]", list.size());
            for (Task task : list) {

                logger.info("待处理任务 [{}]", task.getName());
                Map<String, Object> variables = getMap(processEngine, scanner, task);
                taskService.complete(task.getId(),variables);
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }
        scanner.close();
    }

    /**
     * @Author: xingrui
     * @Description: 获取输入表单
     * @Date: 14:27 2018/8/13
     */
    private static Map<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String,Object> variables = Maps.newHashMap();
        for (FormProperty property : formProperties) {
            String line = null;
            
            if(StringFormType.class.isInstance(property.getType())){
                logger.info("请输入 {} ？",property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(),line);
            }else if(DateFormType.class.isInstance(property.getType())){
                logger.info("请输入 {} ？ 格式 （yyyy-MM-dd）",property.getName());
                line = scanner.nextLine();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(line);
                variables.put(property.getId(),date);
            }else{
                logger.info("类型暂不支持 {}",property.getType());
            }
            logger.info("您输入的内容是 [{}]",line);

        }
        return variables;
    }

    /**
     * @Author: xingrui
     * @Description: 流程运行实例
     * @Date: 14:27 2018/8/13
     */
    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        logger.info("启动流程 [{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    /**
     * @Author: xingrui
     * @Description: 获取流程部署实例
     * @Date: 14:27 2018/8/13
     */
    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        logger.info("流程定义文件 [{}] , 流程ID [{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    /**
     * @Author: xingrui
     * @Description: 获取流程引擎
     * @Date: 14:28 2018/8/13
     */
    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;

        logger.info("流程引擎名称{},版本{}", name, version);
        return processEngine;
    }

}
