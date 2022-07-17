close all

figure
data = input(5000:10000,2:10);
binWidth = 1000;
lastVal = ceil(max(data));
binEdges = 0:binWidth:lastVal+1;
h = histogram(data,binEdges);
xlabel('Makespan');
ylabel('Frequency');

hold on

data = input1(5000:10000,2:10);
binWidth = 1000;
lastVal = ceil(max(data));
binEdges = 0:binWidth:lastVal+1;
h = histogram(data,binEdges);
xlabel('Makespan');
ylabel('Frequency (count)');

legend('WFD','AJLR','FontSize',14,'Location','northeast')



figure;

input_comb = input(5000:10000,2:10);
input_1d = reshape(input_comb,1,[]);
y1 = quantile(input_1d,[0.8 0.9 0.95 0.99 0.999 0.9999 1]);
plot([1 2 3 4 5 6 7],y1,'b-o','LineWidth',1)
hold on 

input_comb1 = input1(5000:10000,2:10);
input_1d1 = reshape(input_comb1,1,[]);
y2 = quantile(input_1d1,[0.8 0.9 0.95 0.99 0.999 0.9999 1]);
plot([1 2 3 4 5 6 7],y2,'r-*','LineWidth',1)
hold on 


set(gca,'xtick',[1 2 3 4 5 6 7]);
set(gca,'xticklabel',{'80%' ,'90%' ,'95%', '99%', '99.9%','99.99%', '100%'},'FontSize', 12);

ylabel("Resulting Makespan",'FontSize', 14 )
xlabel("Probability",'FontSize', 14 )

legend('WFD','AJLR','FontSize',14,'Location','northwest')







% input_comb = input(5000:10000,2:10);
% input_1d = reshape(input_comb,1,[]);
% [f1,x1]=ecdf(input_1d);
% plot(f1,x1,'b','LineWidth',1)
% hold on 
% 
% input_comb1 = input1(5000:10000,2:10);
% input_1d1 = reshape(input_comb1,1,[]);
% [f1,x1]=ecdf(input_1d1);
% plot(f1,x1,'r','LineWidth',1)
% hold on 
% % set(gca, 'XScale', 'log')
% % set(gca, 'YScale', 'log')
% ylabel("Resulting Makespan",'FontSize', 14 )
% xlabel("Probability",'FontSize', 14 )
% 
% legend('WFD','AJLR','FontSize',14,'Location','northwest')