close all
f=figure('Position', [100, 100, 850, 400]);

p=plot(input,'lineWidth',1.5, 'MarkerSize',7);
p(1).Marker = 'o';
p(2).Marker = '*';
p(3).Marker = '+';
p(4).Marker = 'd';


ylim([0,12])
xlim([1,10])


xlabel('Number of stages (S)');
ylabel('Quality ranking');


set(gca,'xticklabel',{'10', '20', '30','40', '50', '60', '70', '80', '90', '100'},'FontSize', 12);

legend('MOEA/D', 'No-Mig','worst+diverse','linkage','Location','southeast','FontSize',12);