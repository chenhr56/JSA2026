close all

figure
data = input3(5000:10000,2:10);
binWidth = 1000;
lastVal = ceil(max(data));
firstVal = ceil(min(data));
binEdges = 0:binWidth:lastVal+1;
h = histogram(data,binEdges);
xlabel('Makespan');
ylabel('Frequency');
% ylim([0 2000]);

hold on

data = input4(5000:10000,2:10);
binWidth = 1000;
lastVal = ceil(max(data));
firstVal = ceil(min(data));
binEdges = 0:binWidth:lastVal+1;
h = histogram(data,binEdges);
xlabel('Makespan (0 not included)');
ylabel('Frequency (count)');
% ylim([0 2000]);

legend('WFD','AJLR','FontSize',14,'Location','northwest')

% counts = histcounts(data,binEdges);
% binCtrs = binEdges(1:end-1) + binWidth/2;
% h.FaceColor = [.9 .9 .9];
% hold on
% plot(binCtrs,counts,'o');



ax = figure;

% [f1,x1]=ecdf(input(5000:10000,1));
% plot(f1,x1,'k','LineWidth',1)
% hold on 


input_comb = input3(5000:10000,2:10);
input_1d = reshape(input_comb,1,[]);
[f1,x1]=ecdf(input_1d);
plot(f1,x1,'b','LineWidth',1)
hold on 

input_comb1 = input4(5000:10000,2:10);
input_1d1 = reshape(input_comb1,1,[]);
[f1,x1]=ecdf(input_1d1);
plot(f1,x1,'r','LineWidth',1)
hold on 
% set(gca, 'XScale', 'log')
% set(gca, 'YScale', 'log')
ylabel("Resulting Makespan",'FontSize', 14 )
xlabel("Probability",'FontSize', 14 )

legend('WFD','AJLR','FontSize',14,'Location','northwest')

ax = figure;

% [f1,x1]=ecdf(input(5000:10000,1));
% plot(f1,x1,'k','LineWidth',1)
% hold on 


input_comb = input3(5000:10000,2:10);
input_1d = reshape(input_comb,1,[]);
[f1,x1]=ecdf(input_1d);
plot(f1,x1,'b','LineWidth',1)
hold on 

input_comb1 = input4(5000:10000,2:10);
input_1d1 = reshape(input_comb1,1,[]);
[f1,x1]=ecdf(input_1d1);
plot(f1,x1,'r','LineWidth',1)
hold on 
set(gca, 'XScale', 'log')
set(gca, 'YScale', 'log')
ylabel("Resulting Makespan",'FontSize', 14 )
xlabel("Probability",'FontSize', 14 )

legend('WFD','AJLR','FontSize',14,'Location','northwest')