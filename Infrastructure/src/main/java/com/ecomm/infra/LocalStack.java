package com.ecomm.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.elasticache.CfnSubnetGroup;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalStack extends Stack {

    private final Vpc vpc;
    private final Cluster cluster;
    private final CfnCacheCluster elastiCacheCluster;

    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();
        DatabaseInstance auth_db = createDatabaseInstance("AuthServiceDB","auth-service-db");
        DatabaseInstance order_db = createDatabaseInstance("OrderServiceDB","order-service-db");
        DatabaseInstance payment_db = createDatabaseInstance("PaymentServiceDB","payment-service-db");
        DatabaseInstance product_db = createDatabaseInstance("ProductServiceDB","product-service-db");

        CfnHealthCheck auth_db_health_check=createCfnHealthCheck(auth_db,"auth-db-health-check");
        CfnHealthCheck order_db_health_check=createCfnHealthCheck(order_db,"order-db-health-check");
        CfnHealthCheck payment_db_health_check=createCfnHealthCheck(payment_db,"payment-db-health-check");
        CfnHealthCheck product_db_health_check=createCfnHealthCheck(product_db,"product-db-health-check");

        CfnCluster kafka=createCfnCluster();
        this.cluster = createEcsCluster();
        this.elastiCacheCluster= createCfnCacheCluster();

        FargateService authService= createFargateService("AuthService","auth-service",
                List.of(8000), auth_db,
                Map.of("JWT_SECRET_KEY","3f9b8c2d9a7e4f1a6c8d9e2f7b1a9c3d5e7f9a2b4c6d8e0f1a3b5c7d9e1f3a5"));

        authService.getNode().addDependency(auth_db_health_check);
        authService.getNode().addDependency(auth_db);

        FargateService paymentService = createFargateService("PaymentService","payment-service",
                List.of(4001),payment_db,
                Map.of("RAZORPAY_KEY_ID","not enabled","RAZORPAY_KEY_SECRET","not enabled","RAZORPAY_ENABLED","false"));

        paymentService.getNode().addDependency(payment_db);
        paymentService.getNode().addDependency(payment_db_health_check);

        FargateService productService = createFargateService("ProductService","product-service",
                List.of(4000),product_db,null);

        productService.getNode().addDependency(product_db);
        productService.getNode().addDependency(product_db_health_check);
        productService.getNode().addDependency(elastiCacheCluster);


        FargateService orderService = createFargateService("OrderService","order-service",
                List.of(9800),order_db,Map.of("PAYMENT_SERVER_ADDRESS","payment-service.ecommerce.local","PAYMENT_SERVER_PORT","9001",
                        "PRODUCT_GRPC_SERVER","product-service.ecommerce.local","PRODUCT_GRPC_PORT","9002"));

        orderService.getNode().addDependency(order_db);
        orderService.getNode().addDependency(order_db_health_check);
        orderService.getNode().addDependency(productService);
        orderService.getNode().addDependency(paymentService);
        orderService.getNode().addDependency(kafka);

        ApplicationLoadBalancedFargateService applicationLoadBalancedFargateService =createApiGateway();
        applicationLoadBalancedFargateService.getNode().addDependency(elastiCacheCluster);

        FargateService prometheusService= createFargateService("PrometheusService","prometheus-prod",List.of(9090),null,null);

        createGrafanaService();

    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this,"Ecommerce").vpcName("Ecommerce").maxAzs(2)
                .build();
    }

   private DatabaseInstance createDatabaseInstance(final String id, final String dbName) {
        return DatabaseInstance.Builder.create(this,dbName)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17)
                        .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2,InstanceSize.MICRO))
                .allocatedStorage(20)
                .databaseName(dbName)
                .credentials(Credentials.fromGeneratedSecret("khagesh"))
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
   }

   private CfnHealthCheck createCfnHealthCheck(DatabaseInstance db,String id) {
        return CfnHealthCheck.Builder.create(this,id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
   }

   private CfnCluster createCfnCluster() {
        return CfnCluster.Builder.create(this,"kafka-service")
                .clusterName("kafka-service")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .clientSubnets(vpc.getPrivateSubnets().stream().map(ISubnet::getSubnetId).toList())
                        .brokerAzDistribution("DEFAULT")
                        .instanceType("kafka.m5.large")
                        .build())
                .build();
   }

   private Cluster createEcsCluster() {
        return Cluster.Builder.create(this,"EcommerceCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("ecommerce.local")
                        .build())
                .build();
   }

   private FargateService createFargateService(String id, String imageName, List<Integer> ports, DatabaseInstance dbInstance, Map<String,String> additionalenvironmentVariables) {
       FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this,imageName+"Task")
               .cpu(256)
               .memoryLimitMiB(512)
               .build();

       ContainerDefinitionOptions.Builder containerdefinationOptions=ContainerDefinitionOptions.builder()
               .containerName(imageName+"Container")
               .image(ContainerImage.fromRegistry(imageName))
               .portMappings(ports.stream().map(port->PortMapping.builder()
                       .containerPort(port)
                       .hostPort(port)
                       .protocol(Protocol.TCP)
                       .build()).toList())
               .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                               .logGroup(LogGroup.Builder.create(this,id+" LogGroup")
                                       .logGroupName("/ecs/"+imageName)
                                       .removalPolicy(RemovalPolicy.DESTROY)
                                       .retention(RetentionDays.ONE_DAY)
                                       .build())
                               .streamPrefix(imageName)
                       .build()));

       Map<String,String> env=new HashMap<>();
       env.put("SPRING_KAFKA_BOOTSTRAP_SERVERS","localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");
       env.put("SPRING_CACHE_TYPE","redis");
       env.put("SPRING_DATA_REDIS_HOST",elastiCacheCluster.getAttrRedisEndpointAddress());
       env.put("SPRING_DATA_REDIS_PORT",elastiCacheCluster.getAttrRedisEndpointPort());
       if(additionalenvironmentVariables!=null) {
           env.putAll(additionalenvironmentVariables);
       }

       if(dbInstance!=null) {
           env.put("SPRING_DATASOURCE_URL","jdbc:postgres://%s:%s/%s-db".formatted(
                   dbInstance.getDbInstanceEndpointAddress(),
                   dbInstance.getDbInstanceEndpointPort(),
                   imageName
           ));
           env.put("SPRING_DATASOURCE_USERNAME","khagesh");
           env.put("SPRING_DATASOURCE_PASSWORD",dbInstance.getSecret().secretValueFromJson("password").toString());
           env.put("SPRING_JPA_HIBERNATE_DDL_AUTO","update");
           env.put("SPRING_SQL_INIT_MODE","always");
           env.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT","60000");
       }
       containerdefinationOptions.environment(env);
       fargateTaskDefinition.addContainer(imageName+"Container", containerdefinationOptions.build());

       return FargateService.Builder
               .create(this,id)
               .cluster(cluster)
               .taskDefinition(fargateTaskDefinition)
               .assignPublicIp(false)
               .cloudMapOptions(CloudMapOptions.builder()
                       .name(imageName)
                       .dnsRecordType(DnsRecordType.A)
                       .build())
               .serviceName(imageName)
               .build();
   }

   private ApplicationLoadBalancedFargateService createApiGateway(){
       FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this,"api-gateway"+"Task")
               .cpu(256)
               .memoryLimitMiB(512)
               .build();


       ContainerDefinitionOptions containerdefinationOptions=ContainerDefinitionOptions.builder()
               .containerName("api-gateway"+"Container")
               .image(ContainerImage.fromRegistry("api-gateway"))
               .environment(Map.of("SPRING_PROFILES_ACTIVE","prod",
                       "SERVICE_URL","http://auth-service.ecommerce.local:8000",
                       "REDIS_HOST",elastiCacheCluster.getAttrRedisEndpointAddress(),
                       "REDIS_PORT",elastiCacheCluster.getAttrRedisEndpointPort()))
               .portMappings(List.of(8080).stream().map(port->PortMapping.builder()
                       .containerPort(port)
                       .hostPort(port)
                       .protocol(Protocol.TCP)
                       .build()).toList())
               .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                       .logGroup(LogGroup.Builder.create(this,"ApiGatewayServiceLogGroup")
                               .logGroupName("/ecs/"+"api-gateway")
                               .removalPolicy(RemovalPolicy.DESTROY)
                               .retention(RetentionDays.ONE_DAY)
                               .build())
                       .streamPrefix("api-gateway")
                       .build()))
               .build();

       fargateTaskDefinition.addContainer("ApiGatewayServiceContainer", containerdefinationOptions);

       ApplicationLoadBalancedFargateService applicationLoadBalancedFargateService= ApplicationLoadBalancedFargateService.Builder.create(this,"ApiGatewayService")
               .cluster(cluster)
               .taskDefinition(fargateTaskDefinition)
               .publicLoadBalancer(true)
               .cloudMapOptions(CloudMapOptions.builder()
                       .name("api-gateway")
                       .dnsRecordType(DnsRecordType.A)
                       .build())
               .serviceName("api-gateway")
               .desiredCount(1)
               .healthCheckGracePeriod(Duration.seconds(60))
               .build();

   return applicationLoadBalancedFargateService;
   }

   private CfnCacheCluster createCfnCacheCluster() {
       CfnSubnetGroup cfnSubnetGroup = CfnSubnetGroup.Builder.create(this,"RedisSubnetGroup")
               .description("Redis/ElasticCache Subnet Group")
               .subnetIds(vpc.getPrivateSubnets().stream().map(ISubnet::getSubnetId).toList())
               .build();

       return CfnCacheCluster.Builder.create(this,"RedisCluster")
               .cacheNodeType("cache.t2.micro")
               .engine("redis")
               .numCacheNodes(1)
               .cacheSubnetGroupName(cfnSubnetGroup.getCacheSubnetGroupName())
               .vpcSecurityGroupIds(List.of(vpc.getVpcDefaultSecurityGroup()))
               .build();

   }

   private ApplicationLoadBalancedFargateService createGrafanaService() {
        FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this,"GrafanaService")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        fargateTaskDefinition.addContainer("GrafanaContainer",ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("grafana/grafana"))
                        .portMappings(List.of(PortMapping.builder().containerPort(3000).build()))
                        .build());

        return ApplicationLoadBalancedFargateService.Builder.create(this,"GrafanaUIService")
                .publicLoadBalancer(true)
                .taskDefinition(fargateTaskDefinition)
                .listenerPort(3000)
                .desiredCount(1)
                .build();
   }

    public static void main(String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app,"localStack", props);
        app.synth();
        System.out.println("App Synthesizing...");
    }


}
